package com.nexusisekai.game.giftcode;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.shop.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Hệ thống Gift Code.
 * Admin tạo code → player nhập in-game/webshop → nhận item/diamond/gold/title/pet/mount
 */
public class GiftCodeManager {

    private static final Logger log = LoggerFactory.getLogger(GiftCodeManager.class);
    private static GiftCodeManager INSTANCE;

    public static synchronized GiftCodeManager getInstance() {
        if (INSTANCE == null) INSTANCE = new GiftCodeManager();
        return INSTANCE;
    }

    // ─────────────────────────────────────────
    // Redeem
    // ─────────────────────────────────────────

    public RedeemResult redeem(long charId, long accountId, int level, String code) {
        code = code.trim().toUpperCase();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Tìm giftcode
            PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM giftcodes WHERE code=? AND is_active=1 FOR UPDATE");
            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return RedeemResult.fail("Mã code không tồn tại hoặc không hợp lệ.");

            int gcId       = rs.getInt("id");
            int maxUses    = rs.getInt("max_uses");
            int usedCount  = rs.getInt("used_count");
            int minLevel   = rs.getInt("min_level");
            Timestamp exp  = rs.getTimestamp("expires_at");

            if (exp != null && exp.before(new Timestamp(System.currentTimeMillis())))
                return RedeemResult.fail("Mã code đã hết hạn.");
            if (maxUses > 0 && usedCount >= maxUses)
                return RedeemResult.fail("Mã code đã hết lượt sử dụng.");
            if (level < minLevel)
                return RedeemResult.fail("Yêu cầu tối thiểu level " + minLevel + ".");

            // Kiểm tra đã dùng chưa
            PreparedStatement used = c.prepareStatement(
                "SELECT id FROM giftcode_usage WHERE giftcode_id=? AND char_id=?");
            used.setInt(1, gcId); used.setLong(2, charId);
            if (used.executeQuery().next())
                return RedeemResult.fail("Bạn đã sử dụng mã code này rồi.");

            // Lấy phần thưởng
            PreparedStatement rewards = c.prepareStatement(
                "SELECT * FROM giftcode_rewards WHERE giftcode_id=?");
            rewards.setInt(1, gcId);
            ResultSet rr = rewards.executeQuery();

            c.setAutoCommit(false);
            List<String> rewardDescs = new ArrayList<>();
            try {
                while (rr.next()) {
                    String type = rr.getString("reward_type");
                    int rewardId = rr.getInt("reward_id");
                    int qty      = rr.getInt("qty");

                    switch (type) {
                        case "item" -> {
                            ItemManager.getInstance().giveItem(charId, rewardId, qty);
                            rewardDescs.add("Item #" + rewardId + " x" + qty);
                        }
                        case "diamond" -> {
                            c.prepareStatement("UPDATE accounts SET diamond=diamond+" + qty +
                                " WHERE id=" + accountId).executeUpdate();
                            c.prepareStatement("INSERT INTO diamond_transactions " +
                                "(account_id,amount,type,ref_id,description) VALUES (" +
                                accountId + "," + qty + ",'giftcode','" + code + "','Giftcode " + code + "')")
                                .executeUpdate();
                            rewardDescs.add(qty + " Diamond");
                        }
                        case "gold" -> {
                            c.prepareStatement("UPDATE characters SET gold=gold+" + qty +
                                " WHERE id=" + charId).executeUpdate();
                            rewardDescs.add(qty + " Vàng");
                        }
                        case "title" -> {
                            c.prepareStatement(
                                "INSERT IGNORE INTO player_titles (char_id,title_id,source) VALUES (" +
                                charId + "," + rewardId + ",'giftcode_" + code + "')").executeUpdate();
                            rewardDescs.add("Danh hiệu #" + rewardId);
                        }
                        case "pet" -> {
                            c.prepareStatement(
                                "INSERT INTO player_pets (char_id,template_id) VALUES (" +
                                charId + "," + rewardId + ")").executeUpdate();
                            rewardDescs.add("Pet #" + rewardId);
                        }
                        case "mount" -> {
                            c.prepareStatement(
                                "INSERT IGNORE INTO player_mounts (char_id,template_id) VALUES (" +
                                charId + "," + rewardId + ")").executeUpdate();
                            rewardDescs.add("Mount #" + rewardId);
                        }
                    }
                }

                // Ghi log usage
                PreparedStatement logPs = c.prepareStatement(
                    "INSERT INTO giftcode_usage (giftcode_id,char_id,account_id) VALUES (?,?,?)");
                logPs.setInt(1, gcId); logPs.setLong(2, charId); logPs.setLong(3, accountId);
                logPs.executeUpdate();

                // Tăng used_count
                c.prepareStatement("UPDATE giftcodes SET used_count=used_count+1 WHERE id=" + gcId)
                    .executeUpdate();

                c.commit();
                log.info("[GIFTCODE] charId={} redeemed code={} rewards={}", charId, code, rewardDescs);
                return RedeemResult.ok(rewardDescs);

            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }

        } catch (RedeemResult.RedeemException re) {
            return RedeemResult.fail(re.getMessage());
        } catch (Exception e) {
            log.error("[GIFTCODE] Error: {}", e.getMessage(), e);
            return RedeemResult.fail("Lỗi server, vui lòng thử lại.");
        }
    }

    // ─────────────────────────────────────────
    // Admin: CRUD
    // ─────────────────────────────────────────

    public int createCode(String code, String name, String description,
                           int maxUses, int minLevel, Timestamp expiresAt,
                           int serverId, String createdBy) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO giftcodes (code,name,description,max_uses,min_level,expires_at," +
                 "server_id,created_by) VALUES (?,?,?,?,?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, code.toUpperCase()); ps.setString(2, name);
            ps.setString(3, description); ps.setInt(4, maxUses);
            ps.setInt(5, minLevel); ps.setTimestamp(6, expiresAt);
            ps.setInt(7, serverId); ps.setString(8, createdBy);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getInt(1) : -1;
        }
    }

    public void addReward(int giftcodeId, String type, int rewardId, int qty) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO giftcode_rewards (giftcode_id,reward_type,reward_id,qty) VALUES (?,?,?,?)")) {
            ps.setInt(1, giftcodeId); ps.setString(2, type);
            ps.setInt(3, rewardId); ps.setInt(4, qty);
            ps.executeUpdate();
        }
    }

    public void deactivateCode(int id) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE giftcodes SET is_active=0 WHERE id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        }
    }

    public List<Map<String,Object>> listCodes() throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT g.*, COUNT(u.id) as use_count FROM giftcodes g " +
                 "LEFT JOIN giftcode_usage u ON u.giftcode_id=g.id " +
                 "GROUP BY g.id ORDER BY g.created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++)
                    row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
        }
        return list;
    }

    /** Tự sinh code ngẫu nhiên */
    public static String generateCode(String prefix, int length) {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(prefix);
        Random r = new Random();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(r.nextInt(chars.length())));
        return sb.toString();
    }

    // ─────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────

    public static class RedeemResult {
        public boolean success; public String message;
        public List<String> rewards;

        public static RedeemResult ok(List<String> r) {
            RedeemResult res = new RedeemResult();
            res.success = true; res.rewards = r;
            res.message = "Nhận thành công: " + String.join(", ", r);
            return res;
        }
        public static RedeemResult fail(String msg) {
            RedeemResult res = new RedeemResult();
            res.success = false; res.message = msg;
            return res;
        }

        static class RedeemException extends RuntimeException {
            RedeemException(String msg) { super(msg); }
        }
    }
}
