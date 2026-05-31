package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.entity.InventoryItem;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.shop.ItemManager;
import com.nexusisekai.game.shop.ItemTemplate;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Xử lý túi đồ, trang bị, shop
 */
public class InventoryHandler {

    private static final Logger log = LoggerFactory.getLogger(InventoryHandler.class);

    // Slot type constants (phải khớp với client)
    public static final int SLOT_WEAPON   = 0;
    public static final int SLOT_ARMOR    = 1;
    public static final int SLOT_HELMET   = 2;
    public static final int SLOT_BOOTS    = 3;
    public static final int SLOT_RING     = 4;
    public static final int SLOT_NECKLACE = 5;

    // ─────────────────────────────────────────
    // C2S Handlers
    // ─────────────────────────────────────────

    /**
     * C2S_INVENTORY_LIST (0x0501) - Client yêu cầu danh sách túi đồ
     */
    public static void handleInventoryList(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;
        sendInventory(session, player);
    }

    /**
     * C2S_USE_ITEM (0x0502) - Dùng item
     * Payload: [int itemInstanceId]
     */
    public static void handleUseItem(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int itemInstanceId = buf.readInt();
        InventoryItem item = player.getInventoryItem(itemInstanceId);

        if (item == null) {
            sendItemError(session, "Không tìm thấy vật phẩm!");
            return;
        }

        ItemTemplate tpl = ItemManager.getInstance().getTemplate(item.getItemId());
        if (tpl == null) return;

        // Chỉ dùng được consumable
        if (!tpl.getType().equals("consumable")) {
            sendItemError(session, "Vật phẩm này không thể sử dụng!");
            return;
        }

        // Áp dụng hiệu ứng
        boolean used = applyConsumable(player, tpl);
        if (!used) {
            sendItemError(session, "HP/MP đã đầy!");
            return;
        }

        // Giảm số lượng hoặc xoá item
        removeOrDecrease(player, item, itemInstanceId);

        // Gửi lại stats mới
        sendPlayerStats(session, player);
        sendInventory(session, player);
        log.debug("Player {} used item {}", player.getName(), tpl.getName());
    }

    /**
     * C2S_EQUIP_ITEM (0x0503) - Trang bị item
     * Payload: [int itemInstanceId][byte slot]
     */
    public static void handleEquipItem(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int itemInstanceId = buf.readInt();
        int slot = buf.readByte() & 0xFF;

        InventoryItem item = player.getInventoryItem(itemInstanceId);
        if (item == null) {
            sendItemError(session, "Không tìm thấy vật phẩm!");
            return;
        }

        ItemTemplate tpl = ItemManager.getInstance().getTemplate(item.getItemId());
        if (tpl == null) return;

        // Kiểm tra type phù hợp với slot
        if (!isSlotCompatible(tpl, slot)) {
            sendItemError(session, "Vật phẩm không phù hợp với slot này!");
            return;
        }

        // Kiểm tra level requirement
        if (player.getLevel() < tpl.getRequiredLevel()) {
            sendItemError(session, "Cần level " + tpl.getRequiredLevel() + " để trang bị!");
            return;
        }

        // Unequip item đang đeo ở slot đó (nếu có)
        Integer oldEquipId = player.getEquippedItemInstanceId(slot);
        if (oldEquipId != null) {
            InventoryItem old = player.getInventoryItem(oldEquipId);
            if (old != null) {
                old.setEquipped(false);
                updateEquipInDb(old.getInstanceId(), false, -1);
            }
        }

        // Equip item mới
        item.setEquipped(true);
        item.setSlot(slot);
        player.setEquippedSlot(slot, itemInstanceId);
        updateEquipInDb(itemInstanceId, true, slot);

        // Gửi lại stats + inventory
        sendPlayerStats(session, player);
        sendInventory(session, player);
        log.debug("Player {} equipped {} at slot {}", player.getName(), tpl.getName(), slot);
    }

    /**
     * C2S_UNEQUIP_ITEM (0x0504) - Tháo trang bị
     * Payload: [byte slot]
     */
    public static void handleUnequipItem(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int slot = buf.readByte() & 0xFF;
        Integer instanceId = player.getEquippedItemInstanceId(slot);

        if (instanceId == null) {
            sendItemError(session, "Không có trang bị ở slot này!");
            return;
        }

        InventoryItem item = player.getInventoryItem(instanceId);
        if (item != null) {
            item.setEquipped(false);
            item.setSlot(-1);
            updateEquipInDb(instanceId, false, -1);
        }
        player.clearEquippedSlot(slot);

        sendPlayerStats(session, player);
        sendInventory(session, player);
    }

    /**
     * C2S_SHOP_OPEN (0x0505) - Mở cửa hàng
     * Payload: [int shopId]
     */
    public static void handleShopOpen(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int shopId = buf.readInt();
        sendShopItems(session, shopId);
    }

    /**
     * C2S_SHOP_BUY (0x0506) - Mua vật phẩm
     * Payload: [int shopId][int itemId][short quantity]
     */
    public static void handleShopBuy(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int shopId  = buf.readInt();
        int itemId  = buf.readInt();
        int qty     = buf.readShort() & 0xFFFF;

        // Kiểm tra item có trong shop không
        int price = getShopItemPrice(shopId, itemId);
        if (price < 0) {
            sendItemError(session, "Shop không bán vật phẩm này!");
            return;
        }

        long totalCost = (long) price * qty;
        if (player.getGold() < totalCost) {
            sendItemError(session, "Không đủ vàng! Cần: " + totalCost + "G");
            return;
        }

        // Trừ vàng
        player.setGold(player.getGold() - totalCost);
        updateGoldInDb(player.getCharId(), player.getGold());

        // Cho item
        ItemManager.getInstance().giveItem(player.getCharId(), itemId, qty, player);

        // Gửi cập nhật
        sendPlayerStats(session, player);
        sendInventory(session, player);

        String tplName = ItemManager.getInstance().getTemplate(itemId) != null
                ? ItemManager.getInstance().getTemplate(itemId).getName() : "vật phẩm";
        sendSystemMessage(session, "Mua thành công " + qty + "x " + tplName);
        log.info("Player {} bought {}x item#{} for {}G", player.getName(), qty, itemId, totalCost);
    }

    /**
     * C2S_SHOP_SELL (0x0507) - Bán vật phẩm
     * Payload: [int itemInstanceId][short quantity]
     */
    public static void handleShopSell(GameSession session, ByteBuf buf) {
        Player player = session.getPlayer();
        if (player == null) return;

        int instanceId = buf.readInt();
        int qty = buf.readShort() & 0xFFFF;

        InventoryItem item = player.getInventoryItem(instanceId);
        if (item == null) {
            sendItemError(session, "Không tìm thấy vật phẩm!");
            return;
        }

        if (item.isEquipped()) {
            sendItemError(session, "Hãy tháo trang bị trước khi bán!");
            return;
        }

        ItemTemplate tpl = ItemManager.getInstance().getTemplate(item.getItemId());
        if (tpl == null) return;

        int sellQty = Math.min(qty, item.getQuantity());
        long earn = (long) tpl.getSellPrice() * sellQty;

        // Cộng vàng
        player.setGold(player.getGold() + earn);
        updateGoldInDb(player.getCharId(), player.getGold());

        // Giảm/xoá item
        removeOrDecrease(player, item, instanceId, sellQty);

        sendPlayerStats(session, player);
        sendInventory(session, player);
        sendSystemMessage(session, "Bán " + sellQty + "x " + tpl.getName() + " được " + earn + "G");
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private static boolean applyConsumable(Player player, ItemTemplate tpl) {
        int hpHeal = tpl.getHpRestore();
        int mpHeal = tpl.getMpRestore();

        if (hpHeal == 0 && mpHeal == 0) return false;
        if (hpHeal > 0 && player.getHp() >= player.getMaxHp() && mpHeal == 0) return false;
        if (mpHeal > 0 && player.getMp() >= player.getMaxMp() && hpHeal == 0) return false;

        player.setHp(Math.min(player.getMaxHp(), player.getHp() + hpHeal));
        player.setMp(Math.min(player.getMaxMp(), player.getMp() + mpHeal));
        return true;
    }

    private static void removeOrDecrease(Player player, InventoryItem item, int instanceId) {
        removeOrDecrease(player, item, instanceId, 1);
    }

    private static void removeOrDecrease(Player player, InventoryItem item, int instanceId, int qty) {
        if (item.getQuantity() <= qty) {
            player.removeInventoryItem(instanceId);
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "DELETE FROM character_inventory WHERE id=?")) {
                ps.setInt(1, instanceId);
                ps.executeUpdate();
            } catch (Exception e) {
                log.error("Delete item error", e);
            }
        } else {
            item.setQuantity(item.getQuantity() - qty);
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "UPDATE character_inventory SET qty=? WHERE id=?")) {
                ps.setInt(1, item.getQuantity());
                ps.setInt(2, instanceId);
                ps.executeUpdate();
            } catch (Exception e) {
                log.error("Update item qty error", e);
            }
        }
    }

    private static boolean isSlotCompatible(ItemTemplate tpl, int slot) {
        return switch (tpl.getType()) {
            case "weapon"   -> slot == SLOT_WEAPON;
            case "armor"    -> slot == SLOT_ARMOR;
            case "helmet"   -> slot == SLOT_HELMET;
            case "boots"    -> slot == SLOT_BOOTS;
            case "ring"     -> slot == SLOT_RING;
            case "necklace" -> slot == SLOT_NECKLACE;
            default -> false;
        };
    }

    private static void updateEquipInDb(int instanceId, boolean equipped, int slot) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE character_inventory SET is_equipped=?, slot=? WHERE id=?")) {
            ps.setBoolean(1, equipped);
            ps.setInt(2, slot);
            ps.setInt(3, instanceId);
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Update equip DB error", e);
        }
    }

    private static void updateGoldInDb(int charId, long gold) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE characters SET gold=? WHERE id=?")) {
            ps.setLong(1, gold);
            ps.setInt(2, charId);
            ps.executeUpdate();
        } catch (Exception e) {
            log.error("Update gold error", e);
        }
    }

    private static int getShopItemPrice(int shopId, int itemId) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT price FROM shop_items WHERE shop_id=? AND item_id=?")) {
            ps.setInt(1, shopId);
            ps.setInt(2, itemId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("price");
        } catch (Exception e) {
            log.error("Get shop price error", e);
        }
        return -1;
    }

    // ─────────────────────────────────────────
    // Packet builders
    // ─────────────────────────────────────────

    public static void sendInventory(GameSession session, Player player) {
        List<InventoryItem> items = player.getInventory();
        ByteBuf out = Unpooled.buffer(256);
        out.writeShort(PacketOpcode.S2C_INVENTORY_LIST);
        out.writeShort(items.size());
        for (InventoryItem item : items) {
            out.writeInt(item.getInstanceId());
            out.writeInt(item.getItemId());
            out.writeShort(item.getQuantity());
            out.writeBoolean(item.isEquipped());
            out.writeByte(item.getSlot());
            // Bonus stats
            out.writeShort(item.getBonusStr());
            out.writeShort(item.getBonusAgi());
            out.writeShort(item.getBonusInt());
            out.writeShort(item.getBonusHp());
            out.writeShort(item.getBonusMp());
            out.writeShort(item.getBonusAtk());
            out.writeShort(item.getBonusDef());
        }
        session.send(out);
    }

    public static void sendPlayerStats(GameSession session, Player player) {
        ByteBuf out = Unpooled.buffer(64);
        out.writeShort(PacketOpcode.S2C_PLAYER_STATS);
        out.writeInt(player.getCharId());
        out.writeInt(player.getHp());
        out.writeInt(player.getMaxHp());
        out.writeInt(player.getMp());
        out.writeInt(player.getMaxMp());
        out.writeLong(player.getGold());
        out.writeInt(player.getLevel());
        out.writeLong(player.getExp());
        out.writeLong(player.getExpToNextLevel());
        out.writeShort(player.getStr());
        out.writeShort(player.getAgi());
        out.writeShort(player.getIntelligence());
        out.writeShort(player.getAttackBonus());
        out.writeShort(player.getDefenseBonus());
        session.send(out);
    }

    private static void sendShopItems(GameSession session, int shopId) {
        List<int[]> items = new ArrayList<>(); // [itemId, price]
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT si.item_id, si.price, i.name, i.type FROM shop_items si " +
                     "JOIN items i ON si.item_id=i.id WHERE si.shop_id=?")) {
            ps.setInt(1, shopId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                items.add(new int[]{rs.getInt("item_id"), rs.getInt("price")});
            }
        } catch (Exception e) {
            log.error("Load shop items error", e);
        }

        ByteBuf out = Unpooled.buffer(64 + 8 * items.size());
        out.writeShort(PacketOpcode.S2C_SHOP_DATA);
        out.writeInt(shopId);
        out.writeShort(items.size());
        for (int[] it : items) {
            out.writeInt(it[0]);  // itemId
            out.writeInt(it[1]);  // price
        }
        session.send(out);
    }

    private static void sendItemError(GameSession session, String msg) {
        byte[] b = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuf out = Unpooled.buffer(4 + b.length);
        out.writeShort(PacketOpcode.S2C_ITEM_ERROR);
        out.writeShort(b.length);
        out.writeBytes(b);
        session.send(out);
    }

    private static void sendSystemMessage(GameSession session, String msg) {
        byte[] b = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuf out = Unpooled.buffer(4 + b.length);
        out.writeShort(PacketOpcode.S2C_SYSTEM_MSG);
        out.writeShort(b.length);
        out.writeBytes(b);
        session.send(out);
    }

    /** C2S_DROP_ITEM: [long instanceId][int qty] */
    public static void handleDropItem(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 12) return;
        long instanceId = buf.readLong();
        int  qty        = buf.readInt();
        try (var c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE character_inventory SET qty=GREATEST(qty-?,0) WHERE id=? AND char_id=?")) {
            ps.setInt(1, qty); ps.setLong(2, instanceId);
            ps.setLong(3, session.getPlayer().getCharId());
            ps.executeUpdate();
            // Clean up zero-qty
            c.prepareStatement("DELETE FROM character_inventory WHERE id=" + instanceId + " AND qty=0").executeUpdate();
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã bỏ vật phẩm.");
        } catch (Exception e) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Lỗi bỏ item."); }
    }
}
