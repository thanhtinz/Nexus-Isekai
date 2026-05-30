package com.nexusisekai.game.guild;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.network.GameNetworkServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildManager {
    private static final Logger log = LoggerFactory.getLogger(GuildManager.class);
    private static GuildManager INSTANCE;
    private final Map<Long, GuildData> cache = new ConcurrentHashMap<>();

    public static synchronized GuildManager getInstance() {
        if (INSTANCE == null) INSTANCE = new GuildManager();
        return INSTANCE;
    }

    // ─── Create / Disband ────────────────────────────────────────

    public long createGuild(long leaderId, String name) throws Exception {
        if (getCharGuildId(leaderId) > 0) throw new IllegalStateException("Bạn đã ở trong guild rồi.");
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO guilds (name,leader_id) VALUES (?,?)",
                    Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name); ps.setLong(2, leaderId);
                ps.executeUpdate();
                long guildId = ps.getGeneratedKeys().getLong(1);

                PreparedStatement mem = c.prepareStatement(
                    "INSERT INTO guild_members (guild_id,char_id,role) VALUES (?,?,1)");
                mem.setLong(1, guildId); mem.setLong(2, leaderId);
                mem.executeUpdate();

                c.prepareStatement("UPDATE characters SET guild_id=" + guildId + " WHERE id=" + leaderId)
                    .executeUpdate();

                c.commit();
                loadGuild(guildId);
                log.info("[GUILD] Created '{}' by charId={}", name, leaderId);
                return guildId;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void disbandGuild(long guildId, long requesterId) throws Exception {
        GuildData g = getGuild(guildId);
        if (g == null) throw new IllegalStateException("Guild không tồn tại.");
        if (g.leaderId != requesterId) throw new IllegalStateException("Chỉ guild leader mới có thể giải tán.");

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                c.prepareStatement("UPDATE characters SET guild_id=0 WHERE guild_id=" + guildId).executeUpdate();
                c.prepareStatement("DELETE FROM guild_members WHERE guild_id=" + guildId).executeUpdate();
                c.prepareStatement("UPDATE guilds SET is_active=0 WHERE id=" + guildId).executeUpdate();
                c.commit();
                cache.remove(guildId);
                // Notify members online
                broadcastGuildMessage(guildId, "Guild đã bị giải tán!");
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
        log.info("[GUILD] Disbanded guildId={}", guildId);
    }

    // ─── Member management ────────────────────────────────────────

    public void invite(long guildId, long inviterId, long targetId) throws Exception {
        GuildData g = getGuild(guildId);
        if (g == null) throw new IllegalStateException("Guild không tồn tại.");
        if (!g.isOfficer(inviterId)) throw new IllegalStateException("Bạn không có quyền mời.");
        if (getCharGuildId(targetId) > 0) throw new IllegalStateException("Người chơi đã có guild.");
        if (g.members.size() >= g.maxMembers) throw new IllegalStateException("Guild đã đầy thành viên.");

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO guild_invites (guild_id,inviter_id,target_id) VALUES (?,?,?)")) {
            ps.setLong(1, guildId); ps.setLong(2, inviterId); ps.setLong(3, targetId);
            ps.executeUpdate();
        }
        // Notify target if online
        var net = GameNetworkServer.getInstance();
        if (net != null) net.getAllSessions().stream()
            .filter(s -> s.getPlayer() != null && s.getPlayer().getCharId() == targetId)
            .findFirst()
            .ifPresent(s -> s.sendError(com.nexusisekai.network.PacketOpcode.S2C_SYSTEM_MSG,
                "Bạn được mời vào guild '" + g.name + "'!"));
    }

    public void acceptInvite(long guildId, long charId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement check = c.prepareStatement(
                 "SELECT id FROM guild_invites WHERE guild_id=? AND target_id=?")) {
            check.setLong(1, guildId); check.setLong(2, charId);
            if (!check.executeQuery().next()) throw new IllegalStateException("Không có lời mời.");
        }
        joinGuild(guildId, charId, 3);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement del = c.prepareStatement(
                 "DELETE FROM guild_invites WHERE target_id=?")) {
            del.setLong(1, charId); del.executeUpdate();
        }
    }

    private void joinGuild(long guildId, long charId, int role) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO guild_members (guild_id,char_id,role) VALUES (?,?,?)");
                ps.setLong(1, guildId); ps.setLong(2, charId); ps.setInt(3, role);
                ps.executeUpdate();
                c.prepareStatement("UPDATE characters SET guild_id=" + guildId + " WHERE id=" + charId)
                    .executeUpdate();
                c.commit();
                loadGuild(guildId);
                broadcastGuildMessage(guildId, getCharName(charId) + " đã tham gia guild!");
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void leave(long guildId, long charId) throws Exception {
        GuildData g = getGuild(guildId);
        if (g == null) throw new IllegalStateException("Guild không tồn tại.");
        if (g.leaderId == charId) throw new IllegalStateException("Leader phải chuyển quyền trước khi rời.");

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                c.prepareStatement("DELETE FROM guild_members WHERE guild_id=" + guildId + " AND char_id=" + charId)
                    .executeUpdate();
                c.prepareStatement("UPDATE characters SET guild_id=0 WHERE id=" + charId).executeUpdate();
                c.commit();
                loadGuild(guildId);
                broadcastGuildMessage(guildId, getCharName(charId) + " đã rời guild.");
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    public void kick(long guildId, long officerId, long targetId) throws Exception {
        GuildData g = getGuild(guildId);
        if (g == null) throw new IllegalStateException("Guild không tồn tại.");
        if (!g.isOfficer(officerId)) throw new IllegalStateException("Bạn không có quyền kick.");
        if (targetId == g.leaderId) throw new IllegalStateException("Không thể kick leader.");
        leave(guildId, targetId);
    }

    public void promote(long guildId, long leaderId, long targetId, int newRole) throws Exception {
        GuildData g = getGuild(guildId);
        if (g == null || g.leaderId != leaderId) throw new IllegalStateException("Chỉ leader mới có thể thăng chức.");
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE guild_members SET role=? WHERE guild_id=? AND char_id=?")) {
            ps.setInt(1, newRole); ps.setLong(2, guildId); ps.setLong(3, targetId);
            ps.executeUpdate();
        }
        loadGuild(guildId);
    }

    // ─── Info ─────────────────────────────────────────────────────

    public GuildData getGuild(long guildId) throws SQLException {
        if (cache.containsKey(guildId)) return cache.get(guildId);
        return loadGuild(guildId);
    }

    private GuildData loadGuild(long guildId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT g.*, COUNT(m.char_id) as member_count FROM guilds g " +
                 "LEFT JOIN guild_members m ON m.guild_id=g.id " +
                 "WHERE g.id=? AND g.is_active=1 GROUP BY g.id")) {
            ps.setLong(1, guildId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) { cache.remove(guildId); return null; }

            GuildData g = new GuildData();
            g.id = guildId; g.name = rs.getString("name");
            g.leaderId = rs.getLong("leader_id");
            g.level = rs.getInt("level"); g.exp = rs.getLong("exp");
            g.gold = rs.getLong("gold"); g.maxMembers = rs.getInt("max_members");
            g.notice = rs.getString("notice"); g.memberCount = rs.getInt("member_count");

            // Load members
            try (PreparedStatement mp = c.prepareStatement(
                 "SELECT m.char_id, m.role, m.contribution, ch.name FROM guild_members m " +
                 "JOIN characters ch ON ch.id=m.char_id WHERE m.guild_id=?")) {
                mp.setLong(1, guildId);
                ResultSet mr = mp.executeQuery();
                g.members = new ArrayList<>();
                while (mr.next()) g.members.add(new GuildMember(
                    mr.getLong("char_id"), mr.getString("name"),
                    mr.getInt("role"), mr.getInt("contribution")));
            }
            cache.put(guildId, g);
            return g;
        }
    }

    public long getCharGuildId(long charId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT guild_id FROM characters WHERE id=?")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    private String getCharName(long charId) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT name FROM characters WHERE id=?")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : "Unknown";
        } catch (Exception e) { return "Unknown"; }
    }

    private void broadcastGuildMessage(long guildId, String msg) {
        var net = GameNetworkServer.getInstance();
        if (net == null) return;
        byte[] msgBytes = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer(4 + msgBytes.length);
        buf.writeShort(com.nexusisekai.network.PacketOpcode.S2C_SYSTEM_MSG);
        buf.writeShort(msgBytes.length); buf.writeBytes(msgBytes);
        net.broadcastToGuild((int) guildId, buf);
    }

    // DTOs
    public static class GuildData {
        public long id, leaderId, exp, gold;
        public int level, maxMembers, memberCount;
        public String name, notice;
        public List<GuildMember> members = new ArrayList<>();
        public boolean isOfficer(long charId) {
            return members.stream().anyMatch(m -> m.charId == charId && m.role <= 2);
        }
    }
    public static class GuildMember {
        public long charId; public String name; public int role, contribution;
        public GuildMember(long id, String n, int r, int c) { charId=id; name=n; role=r; contribution=c; }
    }
}
