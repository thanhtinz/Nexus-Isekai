package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cường hoá trang bị theo hệ thống Ngọc Rồng:
 * +1~+5: tỉ lệ cao, tốn vàng
 * +6~+8: tỉ lệ thấp, tốn diamond
 * +9~+10: rất thấp, thất bại tụt 1 level
 */
public class EnhancementHandler {

    private static final Logger log = LoggerFactory.getLogger(EnhancementHandler.class);

    /**
     * C2S_ENHANCE_ITEM: [long instanceId]
     */
    public static void handleEnhance(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        if (buf.readableBytes() < 8) return;
        long instanceId = buf.readLong();

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                // Lock item row
                PreparedStatement lockPs = c.prepareStatement(
                    "SELECT ci.id, ci.item_id, ci.enhance_level, ci.char_id, " +
                    "       it.item_type, it.name " +
                    "FROM character_inventory ci " +
                    "JOIN items it ON it.id=ci.item_id " +
                    "WHERE ci.id=? AND ci.char_id=? FOR UPDATE");
                lockPs.setLong(1, instanceId);
                lockPs.setLong(2, session.getPlayer().getCharId());
                ResultSet rs = lockPs.executeQuery();
                if (!rs.next()) {
                    c.rollback();
                    session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Vật phẩm không hợp lệ.");
                    return;
                }

                int currentLevel = rs.getInt("enhance_level");
                String itemType  = rs.getString("item_type");
                String itemName  = rs.getString("name");

                // Chỉ cường hoá weapon/armor (type 1,2)
                if (!"1".equals(itemType) && !"2".equals(itemType)) {
                    c.rollback();
                    session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Vật phẩm này không thể cường hoá.");
                    return;
                }
                if (currentLevel >= 10) {
                    c.rollback();
                    session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Trang bị đã đạt cường hoá tối đa +10.");
                    return;
                }

                // Lấy config cho level tiếp theo
                PreparedStatement configPs = c.prepareStatement(
                    "SELECT success_rate, cost_gold, cost_diamond FROM enhancement_config WHERE level=?");
                configPs.setInt(1, currentLevel + 1);
                ResultSet cfg = configPs.executeQuery();
                if (!cfg.next()) { c.rollback(); return; }

                float successRate  = cfg.getFloat("success_rate");
                int   costGold     = cfg.getInt("cost_gold");
                int   costDiamond  = cfg.getInt("cost_diamond");

                // Kiểm tra tài nguyên
                PreparedStatement resPs = c.prepareStatement(
                    "SELECT ch.gold, ac.diamond FROM characters ch " +
                    "JOIN accounts ac ON ac.id=ch.account_id " +
                    "WHERE ch.id=?");
                resPs.setLong(1, session.getPlayer().getCharId());
                ResultSet res = resPs.executeQuery();
                if (!res.next()) { c.rollback(); return; }

                long playerGold    = res.getLong("gold");
                int  playerDiamond = res.getInt("diamond");

                if (playerGold < costGold) {
                    c.rollback();
                    session.sendError(PacketOpcode.S2C_SYSTEM_MSG,
                        String.format("Cần %,d vàng để cường hoá.", costGold));
                    return;
                }
                if (costDiamond > 0 && playerDiamond < costDiamond) {
                    c.rollback();
                    session.sendError(PacketOpcode.S2C_SYSTEM_MSG,
                        String.format("Cần %d diamond để cường hoá.", costDiamond));
                    return;
                }

                // Trừ tài nguyên
                c.prepareStatement("UPDATE characters SET gold=gold-" + costGold +
                    " WHERE id=" + session.getPlayer().getCharId()).executeUpdate();
                if (costDiamond > 0)
                    c.prepareStatement("UPDATE accounts SET diamond=diamond-" + costDiamond +
                        " WHERE id=" + session.getAccountId()).executeUpdate();

                // Roll cường hoá
                float roll = ThreadLocalRandom.current().nextFloat() * 100f;
                boolean success = roll <= successRate;

                int newLevel;
                if (success) {
                    newLevel = currentLevel + 1;
                } else {
                    // +6 trở lên thất bại có thể tụt
                    if (currentLevel >= 6 && ThreadLocalRandom.current().nextFloat() < 0.5f) {
                        newLevel = Math.max(0, currentLevel - 1);
                    } else {
                        newLevel = currentLevel; // giữ nguyên
                    }
                }

                // Cập nhật DB
                c.prepareStatement("UPDATE character_inventory SET enhance_level=" + newLevel +
                    " WHERE id=" + instanceId).executeUpdate();

                c.commit();

                // Gửi kết quả
                sendEnhanceResult(session, instanceId, itemName, currentLevel, newLevel, success);

                // Broadcast nếu đạt +8 trở lên
                if (success && newLevel >= 8)
                    broadcastEnhanceSuccess(session.getPlayer().getName(), itemName, newLevel);

                log.info("[ENHANCE] {} item={} {} -> {} ({})",
                    session.getPlayer().getName(), instanceId, currentLevel, newLevel,
                    success ? "SUCCESS" : "FAIL");

            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }

        } catch (Exception e) {
            log.error("handleEnhance: {}", e.getMessage(), e);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Lỗi cường hoá: " + e.getMessage());
        }
    }

    private static void sendEnhanceResult(GameSession session, long instanceId,
                                            String itemName, int oldLevel, int newLevel, boolean success) {
        String msg;
        if (success) {
            msg = String.format("✓ Cường hoá thành công! %s: +%d → +%d", itemName, oldLevel, newLevel);
        } else if (newLevel < oldLevel) {
            msg = String.format("✗ Cường hoá thất bại! %s tụt xuống +%d", itemName, newLevel);
        } else {
            msg = String.format("✗ Cường hoá thất bại! %s giữ nguyên +%d", itemName, oldLevel);
        }

        ByteBuf buf = Unpooled.buffer(32);
        buf.writeShort(PacketOpcode.S2C_ENHANCE_RESULT);
        buf.writeLong(instanceId);
        buf.writeByte(success ? 1 : (newLevel < oldLevel ? 2 : 0)); // 0=fail_keep,1=success,2=fail_drop
        buf.writeInt(newLevel);
        byte[] msgBytes = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        buf.writeShort(msgBytes.length); buf.writeBytes(msgBytes);
        session.send(buf);
    }

    private static void broadcastEnhanceSuccess(String playerName, String itemName, int level) {
        var net = com.nexusisekai.network.GameNetworkServer.getInstance();
        if (net == null) return;
        String msg = String.format("🌟 %s đã cường hoá %s lên +%d thành công!", playerName, itemName, level);
        byte[] msgBytes = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(4 + msgBytes.length);
        buf.writeShort(PacketOpcode.S2C_SYSTEM_MSG);
        buf.writeShort(msgBytes.length); buf.writeBytes(msgBytes);
        net.broadcastAll(buf);
    }
}
