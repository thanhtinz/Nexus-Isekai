package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.combat.CombatEngine;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.GameNetworkServer;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PvP Duel system:
 *  - Player A gửi lời thách đấu
 *  - Player B chấp nhận/từ chối
 *  - Combat diễn ra bình thường nhưng target là Player
 *  - Kết thúc khi 1 player còn 1 HP (không chết thật)
 */
public class PvPHandler {
    private static final Logger log = LoggerFactory.getLogger(PvPHandler.class);
    // Active duels: key = smallest charId, value = DuelState
    private static final ConcurrentHashMap<Long, DuelState> activeDuels = new ConcurrentHashMap<>();

    /** C2S_PVP_CHALLENGE: [long targetCharId] */
    public static void handleChallenge(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long targetId = buf.readLong();
        long selfId   = session.getPlayer().getCharId();
        if (targetId == selfId) return;

        // Tìm target online
        GameSession targetSession = findSession(targetId);
        if (targetSession == null) {
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Người chơi không online.");
            return;
        }
        if (!WorldManager.getInstance().getMapById(session.getPlayer().getMapId()).isPvp()) {
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Map này không cho phép PvP. Hãy đến map PvP.");
            return;
        }

        // Notify target
        sendPvpRequest(targetSession, session.getPlayer());
        session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Đã gửi lời thách đấu!");
        log.info("[PVP] {} challenged {}", selfId, targetId);
    }

    /** C2S_PVP_RESPOND: [long challengerCharId][byte accept 1/0] */
    public static void handleRespond(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 9) return;
        long challengerId = buf.readLong();
        boolean accept    = buf.readByte() == 1;

        GameSession challengerSession = findSession(challengerId);
        if (challengerSession == null) return;

        if (!accept) {
            challengerSession.sendError(PacketOpcode.S2C_SYSTEM_MSG,
                session.getPlayer().getName() + " đã từ chối thách đấu.");
            return;
        }

        // Bắt đầu duel
        long selfId = session.getPlayer().getCharId();
        long duelKey = Math.min(selfId, challengerId);
        DuelState duel = new DuelState(challengerId, selfId);
        activeDuels.put(duelKey, duel);

        try { saveDuelToDb(duel, session.getPlayer().getMapId()); } catch (Exception e) {}

        // Notify cả 2
        broadcastDuelStart(challengerSession, session, duel.id);
        log.info("[PVP] Duel started: {} vs {}", challengerId, selfId);
    }

    /** C2S_PVP_ATTACK: [long targetCharId][int skillId 0=basic] */
    public static void handlePvpAttack(GameSession session, ByteBuf buf) {
        if (!session.isInGame() || buf.readableBytes() < 8) return;
        long targetId = buf.readLong();
        int  skillId  = buf.readableBytes() >= 4 ? buf.readInt() : 0;

        long selfId  = session.getPlayer().getCharId();
        long duelKey = Math.min(selfId, targetId);
        DuelState duel = activeDuels.get(duelKey);

        if (duel == null || !duel.isParticipant(selfId) || !duel.isParticipant(targetId)) {
            session.sendError(PacketOpcode.S2C_SYSTEM_MSG, "Bạn không trong duel này.");
            return;
        }

        GameSession targetSession = findSession(targetId);
        if (targetSession == null) {
            endDuel(duelKey, selfId, targetId, "disconnect");
            return;
        }

        Player attacker = session.getPlayer();
        Player defender = targetSession.getPlayer();

        // Tính sát thương
        var result = CombatEngine.calculate(attacker, defender, null, skillId);
        int damage = result.damage;

        // Trừ HP nhưng không để xuống 0 (duel kết thúc ở 1HP)
        int newHp = Math.max(1, defender.getHp() - damage);
        defender.setHp(newHp);

        // Gửi combat result đến cả 2
        sendPvpCombatResult(session, targetSession, selfId, targetId, damage, result.isCrit, newHp, defender.getMaxHp());

        // Kiểm tra kết thúc
        if (newHp <= 1) endDuel(duelKey, selfId, targetId, "knockout");
    }

    /** C2S_PVP_SURRENDER */
    public static void handleSurrender(GameSession session, ByteBuf buf) {
        if (!session.isInGame()) return;
        long selfId = session.getPlayer().getCharId();
        // Tìm duel có mình
        activeDuels.entrySet().stream()
            .filter(e -> e.getValue().isParticipant(selfId))
            .findFirst()
            .ifPresent(e -> {
                long opponentId = e.getValue().getOpponent(selfId);
                endDuel(e.getKey(), opponentId, selfId, "surrender");
            });
    }

    // ─── Internal ─────────────────────────────────────────────────

    private static void endDuel(long duelKey, long winnerId, long loserId, String reason) {
        DuelState duel = activeDuels.remove(duelKey);
        if (duel == null) return;

        // Update DB
        try { updateDuelResult(duel.id, winnerId); } catch (Exception e) {}

        // Update PvP stats
        try { updatePvpStats(winnerId, loserId); } catch (Exception e) {}

        // Notify
        GameSession winnerSession = findSession(winnerId);
        GameSession loserSession  = findSession(loserId);

        ActivityHandler.fire(winnerId, "pvp_win", 1);
        ActivityHandler.fire(winnerId, "pvp_kill", 1);
        broadcastDuelEnd(winnerSession, loserSession, winnerId, reason);
        log.info("[PVP] Duel {} ended. Winner={} reason={}", duel.id, winnerId, reason);
    }

    private static void sendPvpRequest(GameSession target, Player challenger) {
        ByteBuf buf = Unpooled.buffer(32);
        buf.writeShort(PacketOpcode.S2C_PVP_REQUEST);
        buf.writeLong(challenger.getCharId());
        byte[] nameBytes = challenger.getName().getBytes(StandardCharsets.UTF_8);
        buf.writeShort(nameBytes.length); buf.writeBytes(nameBytes);
        buf.writeInt(challenger.getLevel());
        target.send(buf);
    }

    private static void broadcastDuelStart(GameSession a, GameSession b, long duelId) {
        ByteBuf buf = Unpooled.buffer(24);
        buf.writeShort(PacketOpcode.S2C_PVP_START);
        buf.writeLong(duelId);
        buf.writeLong(a.getPlayer().getCharId());
        buf.writeLong(b.getPlayer().getCharId());
        ByteBuf buf2 = buf.copy();
        a.send(buf); b.send(buf2);
    }

    private static void sendPvpCombatResult(GameSession attacker, GameSession defender,
                                             long atkId, long defId, int damage, boolean crit,
                                             int defHp, int defMaxHp) {
        ByteBuf buf = Unpooled.buffer(32);
        buf.writeShort(PacketOpcode.S2C_PVP_COMBAT_RESULT);
        buf.writeLong(atkId); buf.writeLong(defId);
        buf.writeInt(damage); buf.writeByte(crit ? 1 : 0);
        buf.writeInt(defHp); buf.writeInt(defMaxHp);
        ByteBuf buf2 = buf.copy();
        attacker.send(buf); defender.send(buf2);
    }

    private static void broadcastDuelEnd(GameSession winner, GameSession loser, long winnerId, String reason) {
        byte[] reasonBytes = reason.getBytes(StandardCharsets.UTF_8);
        for (GameSession s : new GameSession[]{winner, loser}) {
            if (s == null) continue;
            ByteBuf buf = Unpooled.buffer(16 + reasonBytes.length);
            buf.writeShort(PacketOpcode.S2C_PVP_END);
            buf.writeLong(winnerId);
            buf.writeByte(reasonBytes.length); buf.writeBytes(reasonBytes);
            s.send(buf);
        }
    }

    private static GameSession findSession(long charId) {
        var net = GameNetworkServer.getInstance();
        if (net == null) return null;
        return net.getAllSessions().stream()
            .filter(s -> s.getPlayer() != null && s.getPlayer().getCharId() == charId)
            .findFirst().orElse(null);
    }

    private static long saveDuelToDb(DuelState duel, int mapId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO pvp_duels (challenger_id,defender_id,map_id,status,started_at) VALUES (?,?,?,'active',NOW())",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, duel.challengerId); ps.setLong(2, duel.defenderId); ps.setInt(3, mapId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            if (keys.next()) { duel.id = keys.getLong(1); return duel.id; }
            return -1;
        }
    }

    private static void updateDuelResult(long duelId, long winnerId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE pvp_duels SET status='finished',winner_id=?,ended_at=NOW() WHERE id=?")) {
            ps.setLong(1, winnerId); ps.setLong(2, duelId); ps.executeUpdate();
        }
    }

    private static void updatePvpStats(long winnerId, long loserId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Upsert winner
            c.prepareStatement("INSERT INTO pvp_stats (char_id,wins,rating) VALUES (" + winnerId + ",1,1016) " +
                "ON DUPLICATE KEY UPDATE wins=wins+1, rating=LEAST(rating+16,3000)").executeUpdate();
            // Upsert loser
            c.prepareStatement("INSERT INTO pvp_stats (char_id,losses,rating) VALUES (" + loserId + ",1,984) " +
                "ON DUPLICATE KEY UPDATE losses=losses+1, rating=GREATEST(rating-16,0)").executeUpdate();
        }
    }

    public static boolean isInDuel(long charId) {
        return activeDuels.values().stream().anyMatch(d -> d.isParticipant(charId));
    }

    // Inner class
    static class DuelState {
        long id = -1, challengerId, defenderId;
        DuelState(long a, long b) { challengerId=a; defenderId=b; }
        boolean isParticipant(long charId) { return charId==challengerId || charId==defenderId; }
        long getOpponent(long charId) { return charId==challengerId ? defenderId : challengerId; }
    }
}
