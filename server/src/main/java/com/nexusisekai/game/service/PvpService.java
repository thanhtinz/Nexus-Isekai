package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;

/**
 * PvpService — Tính ELO THẬT theo công thức Elo rating chuẩn.
 */
public class PvpService {

    private static final int K_FACTOR = 32; // độ nhạy thay đổi điểm

    /** Tính ELO mới sau trận. Trả về [winnerNewElo, loserNewElo] */
    public static int[] calculateElo(int winnerElo, int loserElo) {
        double expectedWin = 1.0 / (1.0 + Math.pow(10, (loserElo - winnerElo) / 400.0));
        double expectedLose = 1.0 / (1.0 + Math.pow(10, (winnerElo - loserElo) / 400.0));

        int winnerNew = (int) Math.round(winnerElo + K_FACTOR * (1 - expectedWin));
        int loserNew = (int) Math.round(loserElo + K_FACTOR * (0 - expectedLose));

        loserNew = Math.max(0, loserNew); // không âm
        return new int[]{winnerNew, loserNew};
    }

    public static String tierFromElo(int elo) {
        if (elo >= 2400) return "Grandmaster";
        if (elo >= 2100) return "Master";
        if (elo >= 1800) return "Diamond";
        if (elo >= 1500) return "Platinum";
        if (elo >= 1200) return "Gold";
        if (elo >= 900)  return "Silver";
        return "Bronze";
    }

    /** Xử lý kết quả trận đấu, cập nhật ELO + tier cho cả 2 */
    public static void processMatch(long winnerId, long loserId, int seasonId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> w = SqlSafe.queryOne(c,
                "SELECT elo, win_streak FROM pvp_player_season WHERE char_id=? AND season_id=?", winnerId, seasonId);
            Map<String, Object> l = SqlSafe.queryOne(c,
                "SELECT elo FROM pvp_player_season WHERE char_id=? AND season_id=?", loserId, seasonId);

            int wElo = w != null ? ((Number) w.get("elo")).intValue() : 1000;
            int lElo = l != null ? ((Number) l.get("elo")).intValue() : 1000;
            int wStreak = w != null ? ((Number) w.get("win_streak")).intValue() : 0;

            int[] newElos = calculateElo(wElo, lElo);
            int newStreak = wStreak + 1;

            // Bonus streak: +2 ELO mỗi 3 trận thắng liên tiếp
            if (newStreak % 3 == 0) newElos[0] += 2;

            SqlSafe.update(c,
                "INSERT INTO pvp_player_season (char_id, season_id, elo, wins, win_streak, max_streak, tier) VALUES (?,?,?,1,?,?,?) " +
                "ON DUPLICATE KEY UPDATE elo=?, wins=wins+1, win_streak=?, max_streak=GREATEST(max_streak,?), tier=?",
                winnerId, seasonId, newElos[0], newStreak, newStreak, tierFromElo(newElos[0]),
                newElos[0], newStreak, newStreak, tierFromElo(newElos[0]));

            SqlSafe.update(c,
                "INSERT INTO pvp_player_season (char_id, season_id, elo, losses, win_streak, tier) VALUES (?,?,?,1,0,?) " +
                "ON DUPLICATE KEY UPDATE elo=?, losses=losses+1, win_streak=0, tier=?",
                loserId, seasonId, newElos[1], tierFromElo(newElos[1]),
                newElos[1], tierFromElo(newElos[1]));
        }
    }
}
