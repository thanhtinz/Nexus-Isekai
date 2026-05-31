package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/** QuestService — Nhận/cập nhật/hoàn thành nhiệm vụ THẬT */
public class QuestService {

    public static boolean accept(long charId, int questId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> existing = SqlSafe.queryOne(c,
                "SELECT status FROM character_quests WHERE char_id=? AND quest_id=?", charId, questId);
            if (existing != null) return false; // đã nhận
            SqlSafe.update(c,
                "INSERT INTO character_quests (char_id, quest_id, status, progress) VALUES (?,?, 'in_progress', 0)",
                charId, questId);
            return true;
        }
    }

    /** Cập nhật tiến độ (vd: giết quái, thu thập item) */
    public static void updateProgress(long charId, int questId, int amount) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> quest = SqlSafe.queryOne(c,
                "SELECT q.target_count, cq.progress FROM character_quests cq " +
                "JOIN quests q ON q.id=cq.quest_id WHERE cq.char_id=? AND cq.quest_id=? AND cq.status='in_progress'",
                charId, questId);
            if (quest == null) return;

            int target = ((Number) quest.get("target_count")).intValue();
            int progress = Math.min(target, ((Number) quest.get("progress")).intValue() + amount);
            String status = progress >= target ? "completed" : "in_progress";

            SqlSafe.update(c,
                "UPDATE character_quests SET progress=?, status=? WHERE char_id=? AND quest_id=?",
                progress, status, charId, questId);
        }
    }

    /** Trả nhiệm vụ, nhận thưởng */
    public static boolean complete(long charId, int questId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> quest = SqlSafe.queryOne(c,
                "SELECT cq.status, q.reward_type, q.reward_amount, q.exp_reward, q.gold_reward " +
                "FROM character_quests cq JOIN quests q ON q.id=cq.quest_id " +
                "WHERE cq.char_id=? AND cq.quest_id=?", charId, questId);
            if (quest == null || !"completed".equals(quest.get("status"))) return false;

            RewardService.grant(c, charId, "exp", ((Number) quest.getOrDefault("exp_reward", 0)).intValue());
            RewardService.grant(c, charId, "gold", ((Number) quest.getOrDefault("gold_reward", 0)).intValue());
            Object rt = quest.get("reward_type");
            if (rt != null) RewardService.grant(c, charId, (String) rt, ((Number) quest.getOrDefault("reward_amount", 0)).intValue());

            SqlSafe.update(c, "UPDATE character_quests SET status='claimed' WHERE char_id=? AND quest_id=?", charId, questId);
            return true;
        }
    }
}
