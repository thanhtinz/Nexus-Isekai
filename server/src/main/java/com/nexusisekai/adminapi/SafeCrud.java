package com.nexusisekai.adminapi;

import com.nexusisekai.database.SqlSafe;
import java.sql.Connection;
import java.util.*;

/**
 * SafeCrud — Generic CRUD an toàn cho admin, chống SQL injection hoàn toàn.
 *
 * Table + column names được whitelist. Values luôn parameterized.
 * Thay thế ~200 query nối chuỗi rải rác trong AdminApiServer.
 */
public class SafeCrud {

    // Whitelist các bảng admin được phép thao tác
    private static final Set<String> ALLOWED_TABLES = Set.of(
        "items", "classes", "maps", "monsters", "npcs", "quests", "shop_items",
        "gacha_banners", "gacha_pool", "gacha_currencies", "gacha_currency_sources",
        "pvp_seasons", "achievements", "daily_login_rewards", "topup_packages",
        "gift_codes", "news_articles", "social_links", "download_links",
        "game_servers", "server_channels", "intro_scenes", "login_screen_config",
        "tutorial_steps", "audio_assets", "localization_strings", "push_campaigns",
        "sticker_packs", "stickers", "character_expressions", "character_actions",
        "player_interact_menu", "world_bosses", "dungeons", "skills", "mounts",
        "pets", "costumes", "titles", "mission_passes", "protection_config"
    );

    /** SELECT tất cả từ bảng (an toàn — table whitelist) */
    public static List<Map<String,Object>> list(Connection c, String table, String orderBy) throws Exception {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Table not allowed: " + table);
        String order = (orderBy != null && orderBy.matches("[a-zA-Z_]+")) ? " ORDER BY " + orderBy : "";
        return SqlSafe.query(c, "SELECT * FROM " + table + order);
    }

    /** SELECT theo ID */
    public static Map<String,Object> getById(Connection c, String table, int id) throws Exception {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Table not allowed");
        return SqlSafe.queryOne(c, "SELECT * FROM " + table + " WHERE id=?", id);
    }

    /** INSERT — columns whitelist + values parameterized */
    public static long insert(Connection c, String table, Map<String,Object> data) throws Exception {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Table not allowed");
        List<String> cols = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (var e : data.entrySet()) {
            if (e.getKey().matches("[a-zA-Z_]+")) { // column name safe
                cols.add(e.getKey());
                vals.add(e.getValue());
            }
        }
        if (cols.isEmpty()) return -1;
        String placeholders = String.join(",", Collections.nCopies(cols.size(), "?"));
        String sql = "INSERT INTO " + table + " (" + String.join(",", cols) + ") VALUES (" + placeholders + ")";
        return SqlSafe.insert(c, sql, vals.toArray());
    }

    /** UPDATE theo ID */
    public static int update(Connection c, String table, int id, Map<String,Object> data) throws Exception {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Table not allowed");
        List<String> sets = new ArrayList<>();
        List<Object> vals = new ArrayList<>();
        for (var e : data.entrySet()) {
            if (e.getKey().matches("[a-zA-Z_]+") && !e.getKey().equals("id")) {
                sets.add(e.getKey() + "=?");
                vals.add(e.getValue());
            }
        }
        if (sets.isEmpty()) return 0;
        vals.add(id);
        String sql = "UPDATE " + table + " SET " + String.join(",", sets) + " WHERE id=?";
        return SqlSafe.update(c, sql, vals.toArray());
    }

    /** DELETE theo ID */
    public static int delete(Connection c, String table, int id) throws Exception {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Table not allowed");
        return SqlSafe.update(c, "DELETE FROM " + table + " WHERE id=?", id);
    }

    /** Toggle boolean column an toàn */
    public static int toggle(Connection c, String table, int id, String column) throws Exception {
        if (!ALLOWED_TABLES.contains(table)) throw new IllegalArgumentException("Table not allowed");
        if (!column.matches("[a-zA-Z_]+")) throw new IllegalArgumentException("Invalid column");
        return SqlSafe.update(c, "UPDATE " + table + " SET " + column + "=1-" + column + " WHERE id=?", id);
    }
}
