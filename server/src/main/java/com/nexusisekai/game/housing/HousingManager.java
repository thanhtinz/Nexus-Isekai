package com.nexusisekai.game.housing;

import com.nexusisekai.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;

/** Quản lý nhà ở: mua nội thất, đặt vị trí, nâng cấp nhà */
public class HousingManager {
    private static final Logger log = LoggerFactory.getLogger(HousingManager.class);
    private static HousingManager INSTANCE;

    public static synchronized HousingManager getInstance() {
        if (INSTANCE == null) INSTANCE = new HousingManager();
        return INSTANCE;
    }

    public Map<String,Object> getHouseInfo(long charId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT h.*, s.name as spouse_name FROM houses h " +
                 "LEFT JOIN characters s ON s.id=h.spouse_id " +
                 "WHERE h.char_id=?")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            Map<String,Object> row = new LinkedHashMap<>();
            ResultSetMetaData meta = rs.getMetaData();
            for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
            return row;
        }
    }

    public void createHouse(long charId) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO houses (char_id,house_level,house_style) VALUES (?,1,0)")) {
            ps.setLong(1, charId); ps.executeUpdate();
        }
    }

    public List<Map<String,Object>> getFurniture(long charId) throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT hf.*, fc.name, fc.furniture_type, fc.width, fc.height " +
                 "FROM house_furniture hf " +
                 "JOIN houses h ON h.id=hf.house_id " +
                 "JOIN furniture_catalog fc ON fc.id=hf.furniture_id " +
                 "WHERE h.char_id=?")) {
            ps.setLong(1, charId);
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

    public void placeFurniture(long charId, int furnitureId, float x, float y, int rotation) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Lấy houseId
            PreparedStatement housePs = c.prepareStatement("SELECT id FROM houses WHERE char_id=?");
            housePs.setLong(1, charId);
            ResultSet hr = housePs.executeQuery();
            if (!hr.next()) throw new IllegalStateException("Bạn chưa có nhà.");
            long houseId = hr.getLong(1);

            // Kiểm tra đủ tiền
            PreparedStatement fPs = c.prepareStatement(
                "SELECT gold_price, diamond_price FROM furniture_catalog WHERE id=? AND is_active=1");
            fPs.setInt(1, furnitureId);
            ResultSet fr = fPs.executeQuery();
            if (!fr.next()) throw new IllegalStateException("Nội thất không tồn tại.");
            int goldPrice = fr.getInt("gold_price");
            int diamondPrice = fr.getInt("diamond_price");

            if (goldPrice > 0)
                c.prepareStatement("UPDATE characters SET gold=gold-" + goldPrice +
                    " WHERE id=" + charId + " AND gold>=" + goldPrice).executeUpdate();
            if (diamondPrice > 0)
                c.prepareStatement("UPDATE accounts SET diamond=diamond-" + diamondPrice +
                    " WHERE id=(SELECT account_id FROM characters WHERE id=" + charId + ")" +
                    " AND diamond>=" + diamondPrice).executeUpdate();

            // Đặt nội thất
            PreparedStatement ps = c.prepareStatement(
                "INSERT INTO house_furniture (house_id,furniture_id,pos_x,pos_y,rotation) VALUES (?,?,?,?,?)");
            ps.setLong(1, houseId); ps.setInt(2, furnitureId);
            ps.setFloat(3, x); ps.setFloat(4, y); ps.setInt(5, rotation);
            ps.executeUpdate();
        }
    }

    public void removeFurniture(long charId, long furnitureInstanceId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "DELETE hf FROM house_furniture hf " +
                 "JOIN houses h ON h.id=hf.house_id " +
                 "WHERE hf.id=? AND h.char_id=?")) {
            ps.setLong(1, furnitureInstanceId); ps.setLong(2, charId);
            ps.executeUpdate();
        }
    }

    public List<Map<String,Object>> getCatalog() throws SQLException {
        List<Map<String,Object>> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM furniture_catalog WHERE is_active=1 ORDER BY furniture_type, id");
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                list.add(row);
            }
        }
        return list;
    }
}
