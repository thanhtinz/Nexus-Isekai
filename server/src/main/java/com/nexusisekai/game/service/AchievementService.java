package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;

/** AchievementService — Theo dõi + claim thành tựu */
public class AchievementService {

    public static void progress(long charId, int achId, int amount) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> ach = SqlSafe.queryOne(c, "SELECT target FROM achievements WHERE id=?", achId);
            if (ach == null) return;
            int target = ((Number)ach.get("target")).intValue();
            SqlSafe.update(c,
                "INSERT INTO character_achievements (char_id, achievement_id, progress, completed) VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE progress=LEAST(?, progress+?), completed=IF(progress+?>=?,1,completed)",
                charId, achId, Math.min(amount,target), amount>=target?1:0, target, amount, amount, target);
        }
    }

    public static boolean claim(long charId, int achId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String,Object> ca = SqlSafe.queryOne(c,
                "SELECT ca.completed, ca.claimed, a.reward_type, a.reward_amount FROM character_achievements ca " +
                "JOIN achievements a ON a.id=ca.achievement_id WHERE ca.char_id=? AND ca.achievement_id=?", charId, achId);
            if (ca == null || ((Number)ca.get("completed")).intValue()==0 || ((Number)ca.getOrDefault("claimed",1)).intValue()==1)
                return false;
            RewardService.grant(c, charId, (String)ca.get("reward_type"), ((Number)ca.get("reward_amount")).intValue());
            SqlSafe.update(c, "UPDATE character_achievements SET claimed=1 WHERE char_id=? AND achievement_id=?", charId, achId);
            return true;
        }
    }
}
