package com.nexusisekai.game.social;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Hệ thống xã hội: Kết bạn → Hẹn hò → Tình cảm → Cầu hôn → Kết hôn → Con cái
 */
public class SocialManager {

    private static final Logger log = LoggerFactory.getLogger(SocialManager.class);
    private static SocialManager INSTANCE;
    public static synchronized SocialManager getInstance() {
        if (INSTANCE == null) INSTANCE = new SocialManager();
        return INSTANCE;
    }

    // ─────────────────────────────────────────
    // Relationship
    // ─────────────────────────────────────────

    public String getRelationship(long charA, long charB) throws SQLException {
        long a = Math.min(charA, charB), b = Math.max(charA, charB);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT rel_type FROM relationships WHERE char_id_a=? AND char_id_b=?")) {
            ps.setLong(1, a); ps.setLong(2, b);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : "none";
        }
    }

    public void addFriend(long charA, long charB) throws SQLException {
        setRelationship(charA, charB, "friend", 0);
    }

    public void startDating(long charA, long charB) throws SQLException {
        String current = getRelationship(charA, charB);
        if (!current.equals("friend")) throw new IllegalStateException("Phải kết bạn trước khi hẹn hò.");
        setRelationship(charA, charB, "dating", 0);
    }

    public void addAffection(long charA, long charB, int amount) throws SQLException {
        long a = Math.min(charA, charB), b = Math.max(charA, charB);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE relationships SET affection=LEAST(affection+?,1000) WHERE char_id_a=? AND char_id_b=?")) {
            ps.setInt(1, amount); ps.setLong(2, a); ps.setLong(3, b);
            ps.executeUpdate();
        }
    }

    /** Cầu hôn — yêu cầu đang hẹn hò và tình cảm >= 500 */
    public void propose(long proposer, long target, int ringItemId) throws Exception {
        String rel = getRelationship(proposer, target);
        if (!rel.equals("dating")) throw new IllegalStateException("Phải đang hẹn hò mới cầu hôn được.");
        int affection = getAffection(proposer, target);
        if (affection < 500) throw new IllegalStateException("Cần tình cảm >= 500 để cầu hôn.");

        // Tạo engagement
        long a = Math.min(proposer, target), b = Math.max(proposer, target);
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement(
                "INSERT IGNORE INTO marriages (char_id_a,char_id_b,ring_item_id,status) VALUES (" +
                a + "," + b + "," + ringItemId + ",'engaged')").executeUpdate();
            setRelationship(a, b, "engaged", affection);
        }
        log.info("[SOCIAL] Proposed: {} → {}", proposer, target);
    }

    /** Tổ chức đám cưới */
    public void holdWedding(long charA, long charB, int weddingMapId) throws Exception {
        long a = Math.min(charA, charB), b = Math.max(charA, charB);
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement check = c.prepareStatement(
                "SELECT id FROM marriages WHERE char_id_a=? AND char_id_b=? AND status='engaged'");
            check.setLong(1, a); check.setLong(2, b);
            if (!check.executeQuery().next()) throw new IllegalStateException("Chưa đính hôn.");

            c.prepareStatement(
                "UPDATE marriages SET status='married',wedding_date=NOW(),wedding_map=" +
                weddingMapId + " WHERE char_id_a=" + a + " AND char_id_b=" + b).executeUpdate();
            setRelationship(a, b, "married", 1000);
        }
        log.info("[SOCIAL] Wedding: {} ♥ {}", charA, charB);
    }

    public Marriage getMarriage(long charId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM marriages WHERE (char_id_a=? OR char_id_b=?) AND status='married' LIMIT 1")) {
            ps.setLong(1, charId); ps.setLong(2, charId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Marriage m = new Marriage();
            m.id = rs.getLong("id"); m.charIdA = rs.getLong("char_id_a");
            m.charIdB = rs.getLong("char_id_b");
            m.weddingDate = rs.getTimestamp("wedding_date");
            m.status = rs.getString("status");
            return m;
        }
    }

    // ─────────────────────────────────────────
    // Children
    // ─────────────────────────────────────────

    public long createChild(long marriageId, String name, int gender) throws SQLException {
        // Tối đa 3 con
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement count = c.prepareStatement(
                "SELECT COUNT(*) FROM children WHERE marriage_id=?");
            count.setLong(1, marriageId);
            ResultSet rs = count.executeQuery();
            if (rs.next() && rs.getInt(1) >= 3)
                throw new IllegalStateException("Tối đa 3 con cái.");

            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO children (marriage_id,name,gender,age,hp,max_hp,atk,def) VALUES (?,?,?,0,50,50,5,3)",
                Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, marriageId); ps.setString(2, name); ps.setInt(3, gender);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        }
    }

    public List<Child> getChildren(long marriageId) throws SQLException {
        List<Child> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM children WHERE marriage_id=? ORDER BY id")) {
            ps.setLong(1, marriageId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Child ch = new Child();
                ch.id = rs.getLong("id"); ch.name = rs.getString("name");
                ch.gender = rs.getInt("gender"); ch.age = rs.getInt("age");
                ch.level = rs.getInt("level"); ch.hp = rs.getInt("hp");
                ch.maxHp = rs.getInt("max_hp"); ch.atk = rs.getInt("atk");
                ch.def = rs.getInt("def"); ch.skinId = rs.getInt("skin_id");
                ch.isActive = rs.getInt("is_active") == 1;
                ch.happiness = rs.getInt("happiness");
                list.add(ch);
            }
        }
        return list;
    }

    public void feedChild(long childId, long charId) throws SQLException {
        // Tăng happiness khi cho ăn
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE children SET happiness=LEAST(happiness+10,100) WHERE id=?")) {
            ps.setLong(1, childId); ps.executeUpdate();
        }
        // Giảm gold
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE characters SET gold=GREATEST(gold-50,0) WHERE id=?")) {
            ps.setLong(1, charId); ps.executeUpdate();
        }
    }

    public void toggleChildInCombat(long childId, long charId, boolean active) throws SQLException {
        // Chỉ cho 1 con đi chiến đấu cùng lúc
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement(
                "UPDATE children SET is_active=0 WHERE id IN " +
                "(SELECT c.id FROM marriages m JOIN children c ON c.marriage_id=m.id " +
                "WHERE m.char_id_a=" + charId + " OR m.char_id_b=" + charId + ")")
                .executeUpdate();
            if (active) {
                PreparedStatement ps = c.prepareStatement(
                    "UPDATE children SET is_active=1 WHERE id=?");
                ps.setLong(1, childId); ps.executeUpdate();
            }
        }
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private void setRelationship(long charA, long charB, String type, int affection) throws SQLException {
        long a = Math.min(charA, charB), b = Math.max(charA, charB);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO relationships (char_id_a,char_id_b,rel_type,affection) VALUES (?,?,?,?) " +
                 "ON DUPLICATE KEY UPDATE rel_type=VALUES(rel_type)," +
                 "affection=IF(VALUES(affection)>0,VALUES(affection),affection)")) {
            ps.setLong(1, a); ps.setLong(2, b); ps.setString(3, type); ps.setInt(4, affection);
            ps.executeUpdate();
        }
    }

    private int getAffection(long charA, long charB) throws SQLException {
        long a = Math.min(charA, charB), b = Math.max(charA, charB);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT affection FROM relationships WHERE char_id_a=? AND char_id_b=?")) {
            ps.setLong(1, a); ps.setLong(2, b);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // DTOs
    public static class Marriage {
        public long id, charIdA, charIdB; public Timestamp weddingDate; public String status;
    }
    public static class Child {
        public long id; public String name; public int gender, age, level, hp, maxHp, atk, def, skinId, happiness;
        public boolean isActive;
    }
}
