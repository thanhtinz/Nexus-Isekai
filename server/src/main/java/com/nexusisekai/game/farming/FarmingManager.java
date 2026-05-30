package com.nexusisekai.game.farming;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.shop.ItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;

/** Quản lý trang trại: trồng cây, tưới nước, thu hoạch, nuôi động vật */
public class FarmingManager {
    private static final Logger log = LoggerFactory.getLogger(FarmingManager.class);
    private static FarmingManager INSTANCE;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static synchronized FarmingManager getInstance() {
        if (INSTANCE == null) INSTANCE = new FarmingManager();
        return INSTANCE;
    }

    public void start() {
        // Kiểm tra cây chín / động vật cần cho ăn mỗi phút
        scheduler.scheduleAtFixedRate(this::checkGrowth, 1, 1, TimeUnit.MINUTES);
        log.info("[FARM] Farming manager started");
    }

    public void stop() { scheduler.shutdownNow(); }

    // ─── Planting ─────────────────────────────────────────────────

    public void plant(long charId, int plotIndex, int seedItemId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Lấy seed config
            PreparedStatement seedPs = c.prepareStatement(
                "SELECT * FROM farm_seeds WHERE seed_item_id=? AND is_active=1");
            seedPs.setInt(1, seedItemId);
            ResultSet sr = seedPs.executeQuery();
            if (!sr.next()) throw new IllegalStateException("Hạt giống không hợp lệ.");

            int seedId = sr.getInt("id"); int growthTime = sr.getInt("growth_time_min");

            // Kiểm tra ô trống
            PreparedStatement plotPs = c.prepareStatement(
                "SELECT seed_id FROM farm_plots WHERE char_id=? AND plot_index=?");
            plotPs.setLong(1, charId); plotPs.setInt(2, plotIndex);
            ResultSet pr = plotPs.executeQuery();
            if (pr.next() && pr.getInt("seed_id") > 0)
                throw new IllegalStateException("Ô này đã có cây trồng.");

            // Xoá hạt giống từ túi
            c.prepareStatement("UPDATE character_inventory SET qty=qty-1 WHERE char_id=" + charId +
                " AND item_id=" + seedItemId + " AND qty>0").executeUpdate();

            // Trồng cây
            c.prepareStatement("INSERT INTO farm_plots (char_id,plot_index,seed_id,planted_at,stage,water_count) " +
                "VALUES (" + charId + "," + plotIndex + "," + seedId + ",NOW(),1,0) " +
                "ON DUPLICATE KEY UPDATE seed_id=" + seedId + ",planted_at=NOW(),stage=1,water_count=0")
                .executeUpdate();

            log.debug("[FARM] charId={} planted seedId={} at plot {}", charId, seedId, plotIndex);
        }
    }

    public void water(long charId, int plotIndex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT fp.water_count, fs.water_needed, fp.stage, fs.stages " +
                "FROM farm_plots fp JOIN farm_seeds fs ON fs.id=fp.seed_id " +
                "WHERE fp.char_id=? AND fp.plot_index=? AND fp.seed_id>0");
            ps.setLong(1, charId); ps.setInt(2, plotIndex);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không có cây để tưới.");
            if (rs.getInt("stage") >= rs.getInt("stages")) throw new IllegalStateException("Cây đã chín, thu hoạch đi!");

            c.prepareStatement("UPDATE farm_plots SET water_count=water_count+1 WHERE char_id=" + charId +
                " AND plot_index=" + plotIndex).executeUpdate();
        }
    }

    public Map<String,Object> harvest(long charId, int plotIndex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT fp.stage, fs.stages, fs.harvest_item_id, fs.harvest_qty_min, fs.harvest_qty_max " +
                "FROM farm_plots fp JOIN farm_seeds fs ON fs.id=fp.seed_id " +
                "WHERE fp.char_id=? AND fp.plot_index=?");
            ps.setLong(1, charId); ps.setInt(2, plotIndex);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không có cây để thu hoạch.");
            if (rs.getInt("stage") < rs.getInt("stages"))
                throw new IllegalStateException("Cây chưa chín!");

            int itemId = rs.getInt("harvest_item_id");
            int qty = rs.getInt("harvest_qty_min") +
                new Random().nextInt(rs.getInt("harvest_qty_max") - rs.getInt("harvest_qty_min") + 1);

            // Thêm vật phẩm
            ItemManager.getInstance().giveItem(charId, itemId, qty);

            // Xoá ô trống
            c.prepareStatement("UPDATE farm_plots SET seed_id=0,stage=0,water_count=0,planted_at=NULL " +
                "WHERE char_id=" + charId + " AND plot_index=" + plotIndex).executeUpdate();

            return Map.of("item_id", itemId, "qty", qty);
        }
    }

    public List<Map<String,Object>> getFarmState(long charId) throws SQLException {
        List<Map<String,Object>> result = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT fp.plot_index, fp.seed_id, fp.stage, fp.water_count, fp.planted_at, " +
                 "COALESCE(fs.name,'') as seed_name, COALESCE(fs.stages,0) as max_stages, " +
                 "COALESCE(fs.water_needed,0) as water_needed " +
                 "FROM farm_plots fp LEFT JOIN farm_seeds fs ON fs.id=fp.seed_id " +
                 "WHERE fp.char_id=? ORDER BY fp.plot_index")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                for (int i=1; i<=meta.getColumnCount(); i++) row.put(meta.getColumnName(i), rs.getObject(i));
                result.add(row);
            }
        }
        return result;
    }

    // ─── Animals ──────────────────────────────────────────────────

    public void feedAnimal(long charId, int penIndex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT ap.animal_id, fa.feed_item_id FROM animal_pens ap " +
                "JOIN farm_animals fa ON fa.id=ap.animal_id " +
                "WHERE ap.char_id=? AND ap.pen_index=?");
            ps.setLong(1, charId); ps.setInt(2, penIndex);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không có động vật trong chuồng này.");

            int feedItemId = rs.getInt("feed_item_id");
            // Xoá thức ăn từ túi
            int rows = c.prepareStatement("UPDATE character_inventory SET qty=qty-1 WHERE char_id=" + charId +
                " AND item_id=" + feedItemId + " AND qty>0").executeUpdate();
            if (rows == 0) throw new IllegalStateException("Không đủ thức ăn.");

            c.prepareStatement("UPDATE animal_pens SET hunger=LEAST(hunger+30,100) " +
                "WHERE char_id=" + charId + " AND pen_index=" + penIndex).executeUpdate();
        }
    }

    public Map<String,Object> collectAnimalProduct(long charId, int penIndex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT ap.hunger, fa.produce_item_id, fa.produce_qty, fa.produce_time_min " +
                "FROM animal_pens ap JOIN farm_animals fa ON fa.id=ap.animal_id " +
                "WHERE ap.char_id=? AND ap.pen_index=?");
            ps.setLong(1, charId); ps.setInt(2, penIndex);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không có động vật.");
            if (rs.getInt("hunger") < 30) throw new IllegalStateException("Động vật đói quá, hãy cho ăn!");

            int itemId = rs.getInt("produce_item_id");
            int qty    = rs.getInt("produce_qty");
            ItemManager.getInstance().giveItem(charId, itemId, qty);
            c.prepareStatement("UPDATE animal_pens SET hunger=GREATEST(hunger-20,0) " +
                "WHERE char_id=" + charId + " AND pen_index=" + penIndex).executeUpdate();
            return Map.of("item_id", itemId, "qty", qty);
        }
    }

    // ─── Growth tick ──────────────────────────────────────────────

    private void checkGrowth() {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Tăng stage cho cây đã đủ điều kiện (đủ nước + đủ thời gian)
            c.prepareStatement(
                "UPDATE farm_plots fp " +
                "JOIN farm_seeds fs ON fs.id=fp.seed_id " +
                "SET fp.stage = LEAST(fp.stage+1, fs.stages) " +
                "WHERE fp.seed_id>0 AND fp.stage < fs.stages " +
                "AND fp.water_count >= fs.water_needed " +
                "AND TIMESTAMPDIFF(MINUTE, fp.planted_at, NOW()) >= fs.growth_time_min * fp.stage / fs.stages")
                .executeUpdate();
        } catch (Exception e) { log.error("checkGrowth: {}", e.getMessage()); }
    }
}
