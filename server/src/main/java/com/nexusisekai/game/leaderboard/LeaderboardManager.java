package com.nexusisekai.game.leaderboard;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

public class LeaderboardManager {
    private static final Logger log = LoggerFactory.getLogger(LeaderboardManager.class);
    private static LeaderboardManager INSTANCE;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static synchronized LeaderboardManager getInstance() {
        if (INSTANCE == null) INSTANCE = new LeaderboardManager();
        return INSTANCE;
    }

    public void start() {
        // Refresh leaderboard mỗi 10 phút
        scheduler.scheduleAtFixedRate(this::refreshAll, 0, 10, TimeUnit.MINUTES);
        log.info("[LB] LeaderboardManager started");
    }

    public void stop() { scheduler.shutdownNow(); }

    public void refreshAll() {
        try {
            refreshRankType("level",      "ORDER BY ch.level DESC, ch.exp DESC");
            refreshRankType("wealth",     "ORDER BY ch.gold DESC");
            refreshRankType("pvp_rating", "JOIN pvp_stats ps ON ps.char_id=ch.id ORDER BY ps.rating DESC");
            log.debug("[LB] Leaderboard refreshed");
        } catch (Exception e) { log.error("[LB] refresh: {}", e.getMessage()); }
    }

    private void refreshRankType(String rankType, String orderClause) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("DELETE FROM leaderboard_cache WHERE rank_type='" + rankType + "'").executeUpdate();

            String sql = "SELECT ch.id, ch.name, ch.class_id, ch.gender, ch.level, ch.gold, ch.exp " +
                (rankType.equals("pvp_rating") ? ", ps.rating " : "") +
                "FROM characters ch " +
                (rankType.equals("pvp_rating") ? "JOIN pvp_stats ps ON ps.char_id=ch.id " : "") +
                orderClause + " LIMIT 100";

            PreparedStatement ps = c.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();
            int rank = 1;
            while (rs.next()) {
                long rankValue = rankType.equals("level") ? rs.getLong("level") :
                                 rankType.equals("wealth") ? rs.getLong("gold") :
                                 rs.getLong("rating");
                PreparedStatement ins = c.prepareStatement(
                    "INSERT INTO leaderboard_cache (rank_type,rank_pos,char_id,char_name,class_id,gender,rank_value) " +
                    "VALUES (?,?,?,?,?,?,?)");
                ins.setString(1, rankType); ins.setInt(2, rank++);
                ins.setLong(3, rs.getLong("id")); ins.setString(4, rs.getString("name"));
                ins.setInt(5, rs.getInt("class_id")); ins.setInt(6, rs.getInt("gender"));
                ins.setLong(7, rankValue); ins.executeUpdate();
            }
        }
    }

    public List<Map<String,Object>> getLeaderboard(String rankType, int limit) throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT * FROM leaderboard_cache WHERE rank_type=? ORDER BY rank_pos LIMIT ?")) {
            ps.setString(1, rankType); ps.setInt(2, Math.min(limit, 100));
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
        }
        return list;
    }

    public int getPlayerRank(long charId, String rankType) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT rank_pos FROM leaderboard_cache WHERE rank_type=? AND char_id=?")) {
            ps.setString(1, rankType); ps.setLong(2, charId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}
