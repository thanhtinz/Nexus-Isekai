package com.nexusisekai.game.service;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.Map;

/** GuildService — Tạo/gia nhập/rời/đóng góp guild THẬT */
public class GuildService {
    private static final int CREATE_COST_GOLD = 100000;

    public static long create(long charId, String name) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> existing = SqlSafe.queryOne(c, "SELECT id FROM guilds WHERE name=?", name);
            if (existing != null) return -1; // tên trùng

            Map<String, Object> ch = SqlSafe.queryOne(c, "SELECT gold, guild_id FROM characters WHERE id=?", charId);
            if (ch == null || ((Number) ch.get("gold")).longValue() < CREATE_COST_GOLD) return -2; // thiếu gold
            if (ch.get("guild_id") != null && ((Number) ch.get("guild_id")).intValue() > 0) return -3; // đã có guild

            SqlSafe.update(c, "UPDATE characters SET gold=gold-? WHERE id=?", CREATE_COST_GOLD, charId);
            long guildId = SqlSafe.insert(c,
                "INSERT INTO guilds (name, leader_id, level, member_count, created_at) VALUES (?,?,1,1,NOW())",
                name, charId);
            SqlSafe.update(c, "UPDATE characters SET guild_id=?, guild_rank='leader' WHERE id=?", guildId, charId);
            return guildId;
        }
    }

    public static boolean join(long charId, long guildId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> g = SqlSafe.queryOne(c, "SELECT member_count, max_members FROM guilds WHERE id=?", guildId);
            if (g == null) return false;
            int count = ((Number) g.get("member_count")).intValue();
            int max = ((Number) g.getOrDefault("max_members", 50)).intValue();
            if (count >= max) return false; // đầy

            SqlSafe.update(c, "UPDATE characters SET guild_id=?, guild_rank='member' WHERE id=?", guildId, charId);
            SqlSafe.update(c, "UPDATE guilds SET member_count=member_count+1 WHERE id=?", guildId);
            return true;
        }
    }

    public static boolean leave(long charId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            Map<String, Object> ch = SqlSafe.queryOne(c, "SELECT guild_id, guild_rank FROM characters WHERE id=?", charId);
            if (ch == null || ch.get("guild_id") == null) return false;
            long guildId = ((Number) ch.get("guild_id")).longValue();
            if ("leader".equals(ch.get("guild_rank"))) return false; // leader phải chuyển quyền trước

            SqlSafe.update(c, "UPDATE characters SET guild_id=NULL, guild_rank=NULL WHERE id=?", charId);
            SqlSafe.update(c, "UPDATE guilds SET member_count=member_count-1 WHERE id=?", guildId);
            return true;
        }
    }
}
