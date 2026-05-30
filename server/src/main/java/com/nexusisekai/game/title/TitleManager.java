package com.nexusisekai.game.title;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TitleManager {

    private static final Logger log = LoggerFactory.getLogger(TitleManager.class);
    private static TitleManager INSTANCE;
    private final Map<Integer, TitleTemplate> cache = new ConcurrentHashMap<>();

    public static synchronized TitleManager getInstance() {
        if (INSTANCE == null) INSTANCE = new TitleManager();
        return INSTANCE;
    }

    public void loadAll() throws SQLException {
        cache.clear();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM titles WHERE is_active=1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TitleTemplate t = new TitleTemplate(
                    rs.getInt("id"), rs.getString("name"), rs.getString("description"),
                    rs.getString("title_type"), rs.getString("stat_bonus"),
                    rs.getString("color_hex"), rs.getInt("icon_id"));
                cache.put(t.id, t);
            }
        }
        log.info("[TITLE] Loaded {} titles.", cache.size());
    }

    public void grantTitle(long charId, int titleId, String source) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO player_titles (char_id,title_id,source) VALUES (?,?,?)")) {
            ps.setLong(1, charId); ps.setInt(2, titleId); ps.setString(3, source);
            ps.executeUpdate();
        }
    }

    public void equipTitle(long charId, int titleId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("UPDATE player_titles SET is_equipped=0 WHERE char_id=" + charId)
                .executeUpdate();
            PreparedStatement ps = c.prepareStatement(
                "UPDATE player_titles SET is_equipped=1 WHERE char_id=? AND title_id=?");
            ps.setLong(1, charId); ps.setInt(2, titleId);
            ps.executeUpdate();
        }
    }

    public List<PlayerTitle> getPlayerTitles(long charId) throws SQLException {
        List<PlayerTitle> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT pt.*, t.name, t.color_hex FROM player_titles pt " +
                 "JOIN titles t ON t.id=pt.title_id WHERE pt.char_id=? ORDER BY pt.obtained_at DESC")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new PlayerTitle(rs.getInt("title_id"), rs.getString("name"),
                    rs.getString("color_hex"), rs.getInt("is_equipped")==1));
            }
        }
        return list;
    }

    public TitleTemplate getEquippedTitle(long charId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT title_id FROM player_titles WHERE char_id=? AND is_equipped=1 LIMIT 1")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return cache.get(rs.getInt("title_id"));
        }
        return null;
    }

    // Admin CRUD
    public int createTitle(String name, String description, String type,
                            String statBonus, String colorHex, int iconId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO titles (name,description,title_type,stat_bonus,color_hex,icon_id) VALUES (?,?,?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, description);
            ps.setString(3, type); ps.setString(4, statBonus);
            ps.setString(5, colorHex); ps.setInt(6, iconId);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            int newId = keys.next() ? keys.getInt(1) : -1;
            loadAll(); // refresh cache
            return newId;
        }
    }

    public List<Map<String,Object>> listAllTitles() throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT t.*, COUNT(pt.id) as player_count FROM titles t " +
                 "LEFT JOIN player_titles pt ON pt.title_id=t.id GROUP BY t.id ORDER BY t.sort_order");
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

    public static class TitleTemplate {
        public final int id, iconId;
        public final String name, description, type, statBonus, colorHex;
        public TitleTemplate(int id, String name, String desc, String type,
                              String statBonus, String colorHex, int iconId) {
            this.id=id; this.name=name; this.description=desc; this.type=type;
            this.statBonus=statBonus; this.colorHex=colorHex; this.iconId=iconId;
        }
    }

    public static class PlayerTitle {
        public final int titleId; public final String name, colorHex; public final boolean equipped;
        public PlayerTitle(int id, String name, String color, boolean eq) {
            titleId=id; this.name=name; colorHex=color; equipped=eq;
        }
    }
}
