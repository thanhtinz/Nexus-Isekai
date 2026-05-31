package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Map;

/** DailyLoginService — Logic điểm danh 7 ngày + streak THẬT */
public class DailyLoginService {

    public static class ClaimResult {
        public boolean success;
        public int day;
        public String rewardType;
        public int rewardAmount;
        public String message;
    }

    public static ClaimResult claim(long charId) throws Exception {
        ClaimResult r = new ClaimResult();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> status = SqlSafe.queryOne(c,
                "SELECT current_day, last_claim_date FROM daily_login_status WHERE char_id=?", charId);

            LocalDate today = LocalDate.now();
            int currentDay = 1;

            if (status != null) {
                Object lastDate = status.get("last_claim_date");
                int prevDay = ((Number) status.get("current_day")).intValue();
                if (lastDate != null) {
                    LocalDate last = LocalDate.parse(lastDate.toString().substring(0, 10));
                    if (last.equals(today)) {
                        r.success = false; r.message = "Da diem danh hom nay"; return r;
                    }
                    // Liên tiếp → +1, ngắt → reset
                    currentDay = last.equals(today.minusDays(1)) ? (prevDay % 7) + 1 : 1;
                }
            }

            // Lấy reward ngày này
            Map<String, Object> reward = SqlSafe.queryOne(c,
                "SELECT reward_type, reward_amount FROM daily_login_rewards WHERE day_number=?", currentDay);

            r.success = true;
            r.day = currentDay;
            r.rewardType = reward != null ? (String) reward.get("reward_type") : "gold";
            r.rewardAmount = reward != null ? ((Number) reward.get("reward_amount")).intValue() : 1000;
            r.message = "Diem danh ngay " + currentDay;

            // Cập nhật + phát thưởng
            SqlSafe.update(c,
                "INSERT INTO daily_login_status (char_id, current_day, last_claim_date) VALUES (?,?,?) " +
                "ON DUPLICATE KEY UPDATE current_day=?, last_claim_date=?",
                charId, currentDay, today.toString(), currentDay, today.toString());

            RewardService.grant(c, charId, r.rewardType, r.rewardAmount);
            return r;
        }
    }
}
