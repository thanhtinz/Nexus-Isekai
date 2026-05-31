package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;

/** MailService — Gửi/đọc/nhận đính kèm THẬT (per character) */
public class MailService {

    public static void send(long toCharId, String subject, String body, String rewardType, int rewardAmount) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c,
                "INSERT INTO character_mail (char_id, subject, body, reward_type, reward_amount, is_read, claimed, created_at) " +
                "VALUES (?,?,?,?,?,0,0,NOW())",
                toCharId, subject, body, rewardType, rewardAmount);
        }
    }

    /** Gửi mail hàng loạt (system mail cho cả server) */
    public static int broadcast(int serverId, String subject, String body, String rewardType, int rewardAmount) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            return SqlSafe.update(c,
                "INSERT INTO character_mail (char_id, subject, body, reward_type, reward_amount, is_read, claimed, created_at) " +
                "SELECT c.id, ?, ?, ?, ?, 0, 0, NOW() FROM characters c " +
                "JOIN accounts a ON a.id=c.account_id WHERE a.last_server_id=?",
                subject, body, rewardType, rewardAmount, serverId);
        }
    }

    public static boolean claimAttachment(long charId, long mailId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> mail = SqlSafe.queryOne(c,
                "SELECT reward_type, reward_amount, claimed FROM character_mail WHERE id=? AND char_id=?", mailId, charId);
            if (mail == null || ((Number) mail.getOrDefault("claimed", 1)).intValue() == 1) return false;

            Object rt = mail.get("reward_type");
            if (rt != null) RewardService.grant(c, charId, (String) rt, ((Number) mail.get("reward_amount")).intValue());
            SqlSafe.update(c, "UPDATE character_mail SET claimed=1, is_read=1 WHERE id=?", mailId);
            return true;
        }
    }
}
