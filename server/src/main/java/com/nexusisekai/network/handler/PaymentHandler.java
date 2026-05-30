package com.nexusisekai.network.handler;

import com.nexusisekai.game.giftcode.GiftCodeManager;
import com.nexusisekai.game.battlepass.MissionPassManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Handler: gift code in-game, diamond balance, mission pass
 */
public class PaymentHandler {
    private static final Logger log = LoggerFactory.getLogger(PaymentHandler.class);

    // ─── Gift Code ──────────────────────────────

    /**
     * C2S_GIFTCODE: [2b len][code string]
     */
    public static void handleGiftCode(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        int len = buf.readUnsignedShort();
        String code = buf.readCharSequence(len, StandardCharsets.UTF_8).toString();

        var player = session.getPlayer();
        var result = GiftCodeManager.getInstance().redeem(
            player.getCharId(),
            session.getAccountId(),
            player.getLevel(),
            code
        );

        ByteBuf resp = Unpooled.buffer(64);
        if (result.success) {
            resp.writeShort(PacketOpcode.S2C_GIFTCODE_OK);
            byte[] msgBytes = result.message.getBytes(StandardCharsets.UTF_8);
            resp.writeShort(msgBytes.length);
            resp.writeBytes(msgBytes);
        } else {
            resp.writeShort(PacketOpcode.S2C_GIFTCODE_FAIL);
            byte[] msgBytes = result.message.getBytes(StandardCharsets.UTF_8);
            resp.writeShort(msgBytes.length);
            resp.writeBytes(msgBytes);
        }
        session.send(resp);
    }

    // ─── Mission Pass ────────────────────────────

    /**
     * C2S_PASS_INFO: lấy thông tin pass hiện tại
     */
    public static void handlePassInfo(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            var season = MissionPassManager.getInstance().getActiveSeason();
            if (season == null) {
                sendPassInfo(session, null, null);
                return;
            }
            var playerPass = MissionPassManager.getInstance()
                .getPlayerPass(session.getPlayer().getCharId(), season.id);
            var rewards    = MissionPassManager.getInstance().getRewards(season.id);

            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_PASS_INFO);
            resp.writeInt(season.id);
            resp.writeByte(season.isActive ? 1 : 0);
            resp.writeInt(season.freeDiamond);
            resp.writeInt(season.premiumDiamond);
            resp.writeInt(season.maxLevel);

            // Player pass
            resp.writeInt(playerPass.passLevel);
            resp.writeInt(playerPass.passExp);
            resp.writeByte(playerPass.hasPremium ? 1 : 0);

            // Rewards count
            resp.writeShort(rewards.size());
            for (var r : rewards) {
                resp.writeInt(r.level);
                resp.writeByte(r.tier);
                resp.writeInt(r.itemId);
                resp.writeInt(r.itemQty);
                resp.writeInt(r.diamond);
                resp.writeInt(r.gold);
                // Is claimed?
                String key = r.level + "_" + r.tier;
                resp.writeByte(playerPass.claimedRewards.contains(key) ? 1 : 0);
            }
            session.send(resp);
        } catch (Exception e) {
            log.error("handlePassInfo error: {}", e.getMessage(), e);
        }
    }

    /**
     * C2S_PASS_CLAIM: [int level][byte tier]
     */
    public static void handlePassClaim(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        int level = buf.readInt();
        int tier  = buf.readByte();
        try {
            var season = MissionPassManager.getInstance().getActiveSeason();
            if (season == null) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Không có season active."); return; }

            var claims = MissionPassManager.getInstance()
                .claimReward(session.getPlayer().getCharId(), season.id, level, tier);

            ByteBuf resp = Unpooled.buffer(64);
            resp.writeShort(PacketOpcode.S2C_PASS_CLAIM_OK);
            resp.writeInt(level);
            resp.writeByte(tier);
            resp.writeByte(claims.size());
            for (var c : claims) {
                resp.writeByte(c.type.equals("item") ? 1 : c.type.equals("diamond") ? 2 : 3);
                resp.writeInt(c.id);
                resp.writeInt(c.qty);
            }
            session.send(resp);
        } catch (Exception e) {
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage());
        }
    }

    /**
     * C2S_PASS_BUY_PREMIUM: mua premium pass
     */
    public static void handleBuyPremium(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        try {
            var season = MissionPassManager.getInstance().getActiveSeason();
            if (season == null) { session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Không có season active."); return; }

            MissionPassManager.getInstance().buyPremium(
                session.getPlayer().getCharId(),
                session.getAccountId(),
                season.id
            );

            // Refresh diamond display
            sendDiamondUpdate(session);
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Mua Premium Pass thành công!");
        } catch (Exception e) {
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, e.getMessage());
        }
    }

    // ─── Helpers ───────────────────────────────

    public static void sendDiamondUpdate(GameSession session) {
        try (var c = com.nexusisekai.database.DatabaseManager.getInstance().getConnection();
             var ps = c.prepareStatement("SELECT diamond FROM accounts WHERE id=?")) {
            ps.setLong(1, session.getAccountId());
            var rs = ps.executeQuery();
            if (rs.next()) {
                ByteBuf buf = Unpooled.buffer(6);
                buf.writeShort(PacketOpcode.S2C_DIAMOND_UPDATE);
                buf.writeInt(rs.getInt(1));
                session.send(buf);
            }
        } catch (Exception e) { log.error("sendDiamondUpdate: {}", e.getMessage()); }
    }

    private static void sendPassInfo(GameSession session, Object season, Object playerPass) {
        ByteBuf resp = Unpooled.buffer(8);
        resp.writeShort(PacketOpcode.S2C_PASS_INFO);
        resp.writeInt(0); // no active season
        session.send(resp);
    }
}
