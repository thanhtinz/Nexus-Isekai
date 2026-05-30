package com.nexusisekai.game.battlepass;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.shop.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/**
 * Hệ thống Sổ Sứ Mệnh (Battle Pass) — giống Liên Quân Mobile.
 * - Free tier: ai cũng nhận được khi đạt level pass
 * - Premium tier: cần mua pass mới nhận thêm phần thưởng premium
 * - Nhiệm vụ daily/weekly/seasonal để tăng pass EXP
 */
public class MissionPassManager {

    private static final Logger log = LoggerFactory.getLogger(MissionPassManager.class);
    private static MissionPassManager INSTANCE;

    public static synchronized MissionPassManager getInstance() {
        if (INSTANCE == null) INSTANCE = new MissionPassManager();
        return INSTANCE;
    }

    // ─────────────────────────────────────────
    // Season queries
    // ─────────────────────────────────────────

    public Season getActiveSeason() throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM mission_pass_seasons WHERE is_active=1 AND start_date<=CURDATE() AND end_date>=CURDATE() LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? mapSeason(rs) : null;
        }
    }

    public List<Season> listSeasons() throws SQLException {
        List<Season> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM mission_pass_seasons ORDER BY id DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapSeason(rs));
        }
        return list;
    }

    public List<PassReward> getRewards(int seasonId) throws SQLException {
        List<PassReward> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM mission_pass_rewards WHERE season_id=? ORDER BY level, tier")) {
            ps.setInt(1, seasonId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapReward(rs));
        }
        return list;
    }

    public List<PassTask> getTasks(int seasonId, String taskType) throws SQLException {
        List<PassTask> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM mission_pass_tasks WHERE season_id=? AND task_type=? ORDER BY id")) {
            ps.setInt(1, seasonId); ps.setString(2, taskType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapTask(rs));
        }
        return list;
    }

    // ─────────────────────────────────────────
    // Player pass
    // ─────────────────────────────────────────

    public PlayerPass getPlayerPass(long charId, int seasonId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM player_mission_pass WHERE char_id=? AND season_id=?")) {
            ps.setLong(1, charId); ps.setInt(2, seasonId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return mapPlayerPass(rs);
        }
        // Auto-enroll khi chưa có
        return enrollPlayer(charId, seasonId);
    }

    private PlayerPass enrollPlayer(long charId, int seasonId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO player_mission_pass (char_id,season_id,pass_level,pass_exp,has_premium) VALUES (?,?,1,0,0)")) {
            ps.setLong(1, charId); ps.setInt(2, seasonId);
            ps.executeUpdate();
        }
        PlayerPass p = new PlayerPass();
        p.charId = charId; p.seasonId = seasonId; p.passLevel = 1;
        p.passExp = 0; p.hasPremium = false; p.claimedRewards = new ArrayList<>();
        return p;
    }

    public boolean buyPremium(long charId, long accountId, int seasonId) throws Exception {
        Season season = getActiveSeason();
        if (season == null || season.id != seasonId) throw new IllegalStateException("Season không active");
        if (season.premiumDiamond <= 0) throw new IllegalStateException("Season này không có premium");

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.setAutoCommit(false);
            try {
                // Trừ diamond
                PreparedStatement check = c.prepareStatement(
                    "SELECT diamond FROM accounts WHERE id=? FOR UPDATE");
                check.setLong(1, accountId);
                ResultSet rs = check.executeQuery();
                if (!rs.next() || rs.getInt(1) < season.premiumDiamond)
                    throw new IllegalStateException("Không đủ diamond");

                c.prepareStatement(
                    "UPDATE accounts SET diamond=diamond-" + season.premiumDiamond + " WHERE id=" + accountId)
                    .executeUpdate();

                // Log
                c.prepareStatement(
                    "INSERT INTO diamond_transactions (account_id,amount,type,ref_id,description) VALUES (" +
                    accountId + ",-" + season.premiumDiamond + ",'spend','pass_" + seasonId + "','Mua Premium Pass season " + seasonId + "')")
                    .executeUpdate();

                // Cập nhật player pass
                c.prepareStatement(
                    "UPDATE player_mission_pass SET has_premium=1 WHERE char_id=" + charId + " AND season_id=" + seasonId)
                    .executeUpdate();

                c.commit();
                log.info("[PASS] CharId={} bought premium pass season={}", charId, seasonId);
                return true;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /**
     * Thêm pass EXP khi hoàn thành nhiệm vụ hoặc event.
     * Tự động level up pass khi đủ EXP (100 EXP/level).
     */
    public int addPassExp(long charId, int seasonId, int exp) throws SQLException {
        PlayerPass pass = getPlayerPass(charId, seasonId);
        int newExp = pass.passExp + exp;
        int expPerLevel = 100;
        int levelsGained = newExp / expPerLevel;
        int remainExp   = newExp % expPerLevel;
        int newLevel    = Math.min(pass.passLevel + levelsGained, 100);

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "UPDATE player_mission_pass SET pass_level=?,pass_exp=? WHERE char_id=? AND season_id=?")) {
            ps.setInt(1, newLevel); ps.setInt(2, remainExp);
            ps.setLong(3, charId); ps.setInt(4, seasonId);
            ps.executeUpdate();
        }
        return levelsGained;
    }

    /**
     * Nhận phần thưởng tại level cụ thể.
     */
    public List<RewardClaim> claimReward(long charId, int seasonId, int level, int tier) throws Exception {
        PlayerPass pass = getPlayerPass(charId, seasonId);
        if (pass.passLevel < level) throw new IllegalStateException("Chưa đạt level " + level);
        if (tier == 1 && !pass.hasPremium) throw new IllegalStateException("Cần mua premium pass");

        String rewardKey = level + "_" + tier;
        if (pass.claimedRewards.contains(rewardKey))
            throw new IllegalStateException("Đã nhận thưởng này rồi");

        // Lấy reward
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT * FROM mission_pass_rewards WHERE season_id=? AND level=? AND tier=?");
            ps.setInt(1, seasonId); ps.setInt(2, level); ps.setInt(3, tier);
            ResultSet rs = ps.executeQuery();

            List<RewardClaim> claims = new ArrayList<>();
            c.setAutoCommit(false);
            try {
                while (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    int qty    = rs.getInt("item_qty");
                    int diamond = rs.getInt("diamond");
                    int gold   = rs.getInt("gold");

                    if (itemId > 0 && qty > 0) {
                        ItemManager.getInstance().giveItem(charId, itemId, qty);
                        claims.add(new RewardClaim("item", itemId, qty));
                    }
                    if (diamond > 0) {
                        c.prepareStatement(
                            "UPDATE accounts SET diamond=diamond+" + diamond +
                            " WHERE id=(SELECT account_id FROM characters WHERE id=" + charId + ")")
                            .executeUpdate();
                        claims.add(new RewardClaim("diamond", 0, diamond));
                    }
                    if (gold > 0) {
                        c.prepareStatement(
                            "UPDATE characters SET gold=gold+" + gold + " WHERE id=" + charId)
                            .executeUpdate();
                        claims.add(new RewardClaim("gold", 0, gold));
                    }
                }

                // Lưu claimed state
                pass.claimedRewards.add(rewardKey);
                String json = "[\"" + String.join("\",\"", pass.claimedRewards) + "\"]";
                PreparedStatement upd = c.prepareStatement(
                    "UPDATE player_mission_pass SET claimed_rewards=? WHERE char_id=? AND season_id=?");
                upd.setString(1, json); upd.setLong(2, charId); upd.setInt(3, seasonId);
                upd.executeUpdate();

                c.commit();
                return claims;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { c.setAutoCommit(true); }
        }
    }

    /**
     * Cập nhật tiến độ nhiệm vụ khi player làm gì đó (kill monster, login, v.v.)
     */
    public void trackProgress(long charId, String conditionType, int amount) throws SQLException {
        Season season;
        try { season = getActiveSeason(); } catch (Exception e) { return; }
        if (season == null) return;

        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement tasks = c.prepareStatement(
                "SELECT t.id, t.target, t.exp_reward, t.task_type, p.progress, p.is_completed, p.reset_date " +
                "FROM mission_pass_tasks t " +
                "LEFT JOIN player_pass_task_progress p ON p.task_id=t.id AND p.char_id=? " +
                "WHERE t.season_id=? AND t.task_condition LIKE ?");
            tasks.setLong(1, charId); tasks.setInt(2, season.id);
            tasks.setString(3, conditionType + "%");
            ResultSet rs = tasks.executeQuery();

            while (rs.next()) {
                int taskId      = rs.getInt("t.id");
                int target      = rs.getInt("target");
                int expReward   = rs.getInt("exp_reward");
                String taskType = rs.getString("task_type");
                int progress    = rs.getInt("progress");
                boolean done    = rs.getInt("is_completed") == 1;
                if (done) continue;

                // Check daily/weekly reset
                java.sql.Date resetDate = rs.getDate("reset_date");
                boolean needsReset = false;
                if (taskType.equals("daily") && (resetDate == null ||
                    !resetDate.toLocalDate().isEqual(java.time.LocalDate.now())))
                    needsReset = true;
                if (taskType.equals("weekly") && (resetDate == null ||
                    java.time.LocalDate.now().toEpochDay() - resetDate.toLocalDate().toEpochDay() >= 7))
                    needsReset = true;
                if (needsReset) progress = 0;

                int newProgress = Math.min(progress + amount, target);
                boolean completed = newProgress >= target;

                c.prepareStatement(
                    "INSERT INTO player_pass_task_progress (char_id,task_id,season_id,progress,is_completed,reset_date) " +
                    "VALUES (" + charId + "," + taskId + "," + season.id + "," + newProgress + "," +
                    (completed?1:0) + ",CURDATE()) " +
                    "ON DUPLICATE KEY UPDATE progress=" + newProgress + ",is_completed=" + (completed?1:0) +
                    ",reset_date=CURDATE()").executeUpdate();

                if (completed) {
                    addPassExp(charId, season.id, expReward);
                    log.debug("[PASS] charId={} completed task={} gained {} pass exp", charId, taskId, expReward);
                }
            }
        }
    }

    // ─────────────────────────────────────────
    // Admin: season management
    // ─────────────────────────────────────────

    public void createSeason(String name, String description, java.time.LocalDate start,
                              java.time.LocalDate end, int freeDiamond, int premiumDiamond,
                              int maxLevel) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO mission_pass_seasons (name,description,start_date,end_date," +
                 "free_diamond,premium_diamond,max_level,is_active) VALUES (?,?,?,?,?,?,?,0)")) {
            ps.setString(1, name); ps.setString(2, description);
            ps.setDate(3, java.sql.Date.valueOf(start)); ps.setDate(4, java.sql.Date.valueOf(end));
            ps.setInt(5, freeDiamond); ps.setInt(6, premiumDiamond); ps.setInt(7, maxLevel);
            ps.executeUpdate();
        }
    }

    public void activateSeason(int seasonId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("UPDATE mission_pass_seasons SET is_active=0").executeUpdate();
            PreparedStatement ps = c.prepareStatement(
                "UPDATE mission_pass_seasons SET is_active=1 WHERE id=?");
            ps.setInt(1, seasonId); ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────
    // Mappers
    // ─────────────────────────────────────────

    private Season mapSeason(ResultSet rs) throws SQLException {
        Season s = new Season();
        s.id = rs.getInt("id"); s.name = rs.getString("name");
        s.description = rs.getString("description");
        s.startDate = rs.getDate("start_date").toLocalDate();
        s.endDate = rs.getDate("end_date").toLocalDate();
        s.freeDiamond = rs.getInt("free_diamond");
        s.premiumDiamond = rs.getInt("premium_diamond");
        s.maxLevel = rs.getInt("max_level");
        s.isActive = rs.getInt("is_active") == 1;
        return s;
    }

    private PassReward mapReward(ResultSet rs) throws SQLException {
        PassReward r = new PassReward();
        r.id = rs.getInt("id"); r.seasonId = rs.getInt("season_id");
        r.level = rs.getInt("level"); r.tier = rs.getInt("tier");
        r.itemId = rs.getInt("item_id"); r.itemQty = rs.getInt("item_qty");
        r.diamond = rs.getInt("diamond"); r.gold = rs.getInt("gold");
        r.description = rs.getString("description");
        return r;
    }

    private PassTask mapTask(ResultSet rs) throws SQLException {
        PassTask t = new PassTask();
        t.id = rs.getInt("id"); t.seasonId = rs.getInt("season_id");
        t.taskType = rs.getString("task_type"); t.title = rs.getString("title");
        t.description = rs.getString("description");
        t.taskCondition = rs.getString("task_condition");
        t.target = rs.getInt("target"); t.expReward = rs.getInt("exp_reward");
        return t;
    }

    private PlayerPass mapPlayerPass(ResultSet rs) throws SQLException {
        PlayerPass p = new PlayerPass();
        p.charId = rs.getLong("char_id"); p.seasonId = rs.getInt("season_id");
        p.passLevel = rs.getInt("pass_level"); p.passExp = rs.getInt("pass_exp");
        p.hasPremium = rs.getInt("has_premium") == 1;
        p.claimedRewards = new ArrayList<>();
        String json = rs.getString("claimed_rewards");
        if (json != null && !json.isEmpty()) {
            json = json.replaceAll("[\\[\\]\"]", "");
            for (String s : json.split(",")) if (!s.trim().isEmpty()) p.claimedRewards.add(s.trim());
        }
        return p;
    }

    // ─────────────────────────────────────────
    // DTOs
    // ─────────────────────────────────────────

    public static class Season {
        public int id, freeDiamond, premiumDiamond, maxLevel;
        public String name, description;
        public java.time.LocalDate startDate, endDate;
        public boolean isActive;
    }

    public static class PassReward {
        public int id, seasonId, level, tier, itemId, itemQty, diamond, gold;
        public String description;
    }

    public static class PassTask {
        public int id, seasonId, target, expReward;
        public String taskType, title, description, taskCondition;
    }

    public static class PlayerPass {
        public long charId; public int seasonId, passLevel, passExp;
        public boolean hasPremium;
        public List<String> claimedRewards;
    }

    public static class RewardClaim {
        public String type; public int id, qty;
        public RewardClaim(String t, int i, int q) { type=t; id=i; qty=q; }
    }
}
