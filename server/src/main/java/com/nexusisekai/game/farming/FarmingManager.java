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
                "ON DUPLICATE KEY UPDATE seed_id=" + seedId + ",planted_at=NOW(),stage=1,water_count=0,health=100,fertilized=0,last_water=NOW()")
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

            c.prepareStatement("UPDATE farm_plots SET water_count=water_count+1, last_water=NOW(), " +
                "health=LEAST(100,health+10) WHERE char_id=" + charId + " AND plot_index=" + plotIndex).executeUpdate();
        }
    }

    public Map<String,Object> harvest(long charId, int plotIndex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT fp.stage, fp.health, fp.fertilized, fs.stages, fs.harvest_item_id, fs.harvest_qty_min, fs.harvest_qty_max " +
                "FROM farm_plots fp JOIN farm_seeds fs ON fs.id=fp.seed_id " +
                "WHERE fp.char_id=? AND fp.plot_index=?");
            ps.setLong(1, charId); ps.setInt(2, plotIndex);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không có cây để thu hoạch.");
            if (rs.getInt("stage") < rs.getInt("stages"))
                throw new IllegalStateException("Cây chưa chín!");

            int itemId = rs.getInt("harvest_item_id");
            int baseQty = rs.getInt("harvest_qty_min") +
                new Random().nextInt(rs.getInt("harvest_qty_max") - rs.getInt("harvest_qty_min") + 1);
            // Sản lượng phụ thuộc SỨC KHOẺ cây (giống Avatar: base * health/100), bón phân +20%
            int health = rs.getInt("health");
            boolean fert = rs.getInt("fertilized") == 1;
            int qty = Math.max(1, baseQty * health / 100);
            if (fert) qty = (int)(qty * 1.2);

            ItemManager.getInstance().giveItem(charId, itemId, qty);

            // Xoá ô trống
            c.prepareStatement("UPDATE farm_plots SET seed_id=0,stage=0,water_count=0,planted_at=NULL,health=100,fertilized=0 " +
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

            c.prepareStatement("UPDATE animal_pens SET hunger=LEAST(hunger+30,100), " +
                "health=LEAST(health+10,100), last_fed=NOW() " +
                "WHERE char_id=" + charId + " AND pen_index=" + penIndex + " AND is_alive=1").executeUpdate();
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


    /** Bón phân: giảm thời gian trưởng thành + tăng sản lượng cuối (giống Avatar isFertilized). */
    public void fertilize(long charId, int plotIndex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT fs.fertilizer_item_id FROM farm_plots fp JOIN farm_seeds fs ON fs.id=fp.seed_id " +
                "WHERE fp.char_id=? AND fp.plot_index=? AND fp.seed_id>0 AND fp.fertilized=0");
            ps.setLong(1, charId); ps.setInt(2, plotIndex);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không thể bón phân (chưa trồng hoặc đã bón).");
            int fertItem = rs.getInt("fertilizer_item_id");
            if (fertItem > 0) {
                int rows = c.prepareStatement("UPDATE character_inventory SET qty=qty-1 WHERE char_id=" + charId +
                    " AND item_id=" + fertItem + " AND qty>0").executeUpdate();
                if (rows == 0) throw new IllegalStateException("Không đủ phân bón.");
            }
            c.prepareStatement("UPDATE farm_plots SET fertilized=1, health=LEAST(100,health+15) " +
                "WHERE char_id=" + charId + " AND plot_index=" + plotIndex).executeUpdate();
        }
    }

    /** Sinh sản: thú sẵn sàng → tạo trứng/con vào chuồng trống hoặc nhận item con giống. */
    public Map<String,Object> breedAnimal(long charId, int penIndex) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT ap.animal_id, ap.breed_ready, fa.produce_item_id FROM animal_pens ap " +
                "JOIN farm_animals fa ON fa.id=ap.animal_id " +
                "WHERE ap.char_id=? AND ap.pen_index=? AND ap.is_alive=1");
            ps.setLong(1, charId); ps.setInt(2, penIndex);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Không có thú.");
            if (rs.getInt("breed_ready") != 1) throw new IllegalStateException("Thú chưa sẵn sàng sinh sản.");

            int animalId = rs.getInt("animal_id");
            // Reset chu kỳ sinh sản
            c.prepareStatement("UPDATE animal_pens SET breed_ready=0, last_fed=NOW() " +
                "WHERE char_id=" + charId + " AND pen_index=" + penIndex).executeUpdate();

            // Tìm chuồng trống để đặt con mới
            PreparedStatement emptyPs = c.prepareStatement(
                "SELECT pen_index FROM animal_pens WHERE char_id=? AND (animal_id=0 OR is_alive=0) ORDER BY pen_index LIMIT 1");
            emptyPs.setLong(1, charId);
            ResultSet er = emptyPs.executeQuery();
            if (er.next()) {
                int freePen = er.getInt("pen_index");
                c.prepareStatement("UPDATE animal_pens SET animal_id=" + animalId +
                    ",is_alive=1,health=100,hunger=100,level=1,breed_ready=0,produce_ready_at=NULL,last_fed=NOW() " +
                    "WHERE char_id=" + charId + " AND pen_index=" + freePen).executeUpdate();
                return Map.of("type", "offspring", "pen_index", freePen, "animal_id", animalId);
            } else {
                // Hết chuồng → nhận item con giống
                ItemManager.getInstance().giveItem(charId, rs.getInt("produce_item_id"), 1);
                return Map.of("type", "item", "item_id", rs.getInt("produce_item_id"), "qty", 1);
            }
        }
    }

    /** Thăm vườn người khác (chỉ xem, không tương tác). */
    public List<Map<String,Object>> visitFarm(long ownerCharId) throws SQLException {
        return getFarmState(ownerCharId);
    }

    // ─── Growth tick ──────────────────────────────────────────────

    private void checkGrowth() {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // 1. Cây HÉO: quá 30 phút không tưới → giảm sức khoẻ (giảm sản lượng cuối)
            c.prepareStatement(
                "UPDATE farm_plots SET health=GREATEST(0, health-10) " +
                "WHERE seed_id>0 AND last_water IS NOT NULL " +
                "AND TIMESTAMPDIFF(MINUTE, last_water, NOW()) > 30")
                .executeUpdate();

            // 2. Tăng stage (đủ nước + đủ thời gian). Bón phân giảm 25% thời gian cần.
            c.prepareStatement(
                "UPDATE farm_plots fp " +
                "JOIN farm_seeds fs ON fs.id=fp.seed_id " +
                "SET fp.stage = LEAST(fp.stage+1, fs.stages) " +
                "WHERE fp.seed_id>0 AND fp.stage < fs.stages " +
                "AND fp.water_count >= fs.water_needed " +
                "AND TIMESTAMPDIFF(MINUTE, fp.planted_at, NOW()) >= " +
                "  (fs.growth_time_min * (CASE WHEN fp.fertilized=1 THEN 75 ELSE 100 END) / 100) * fp.stage / fs.stages")
                .executeUpdate();

            // 3. THÚ: đói dần (hunger giảm), sức khoẻ giảm khi đói lâu
            c.prepareStatement(
                "UPDATE animal_pens SET hunger=GREATEST(0, hunger-5) WHERE is_alive=1 AND animal_id>0")
                .executeUpdate();
            c.prepareStatement(
                "UPDATE animal_pens SET health=GREATEST(0, health-10) WHERE is_alive=1 AND animal_id>0 AND hunger<=0")
                .executeUpdate();

            // 4. THÚ CHẾT nếu sức khoẻ = 0 (bỏ bê quá lâu)
            c.prepareStatement(
                "UPDATE animal_pens SET is_alive=0 WHERE is_alive=1 AND health<=0")
                .executeUpdate();

            // 5. THÚ sẵn sàng sinh sản khi đủ thời gian + khoẻ mạnh
            c.prepareStatement(
                "UPDATE animal_pens ap JOIN farm_animals fa ON fa.id=ap.animal_id " +
                "SET ap.breed_ready=1 " +
                "WHERE ap.is_alive=1 AND ap.breed_ready=0 AND ap.health>=80 AND ap.hunger>=50 " +
                "AND ap.last_fed IS NOT NULL AND TIMESTAMPDIFF(MINUTE, ap.last_fed, NOW()) >= fa.breed_time_min")
                .executeUpdate();
        } catch (Exception e) { log.error("checkGrowth: {}", e.getMessage()); }
    }
}
