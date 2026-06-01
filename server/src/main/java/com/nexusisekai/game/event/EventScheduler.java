package com.nexusisekai.game.event;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.PacketOpcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.concurrent.*;

/**
 * Scheduler cho các sự kiện game: double EXP, boss xuất hiện, sự kiện theo giờ,...
 */
public class EventScheduler {

    private static final Logger log = LoggerFactory.getLogger(EventScheduler.class);
    private static final EventScheduler INSTANCE = new EventScheduler();
    public static EventScheduler getInstance() { return INSTANCE; }

    private ScheduledExecutorService scheduler;
    private WorldManager world;

    private EventScheduler() {}

    public void start(WorldManager world) {
        this.world = world;
        scheduler = Executors.newScheduledThreadPool(2);

        // Kiểm tra events mỗi phút
        scheduler.scheduleAtFixedRate(this::checkScheduledEvents, 0, 60, TimeUnit.SECONDS);

        // Ping tất cả client mỗi 30s
        scheduler.scheduleAtFixedRate(this::pingAllClients, 30, 30, TimeUnit.SECONDS);

        // Lưu DB tất cả player đang online mỗi 5 phút
        scheduler.scheduleAtFixedRate(this::autosaveAllPlayers, 5, 5, TimeUnit.MINUTES);

        // Poll cross-server relay mỗi 10 giây
        scheduler.scheduleAtFixedRate(this::pollCrossServerRelay, 10, 10, TimeUnit.SECONDS);

        // Chăm con: giảm nhu cầu dần, bảo mẫu tự chăm (mỗi 10 phút)
        scheduler.scheduleAtFixedRate(this::childCareTask, 10, 10, TimeUnit.MINUTES);

        // World Boss: spawn theo chu kỳ + despawn khi hết giờ (mỗi 30s)
        scheduler.scheduleAtFixedRate(this::worldBossTask, 20, 30, TimeUnit.SECONDS);

        // Guild War: chuyển trạng thái scheduled→ongoing→ended + trao thưởng (mỗi 30s)
        scheduler.scheduleAtFixedRate(this::guildWarTask, 25, 30, TimeUnit.SECONDS);

        // Hoạt Động đua top: phát thưởng theo hạng khi sự kiện kết thúc (mỗi 60s)
        scheduler.scheduleAtFixedRate(this::activityRankingTask, 40, 60, TimeUnit.SECONDS);

        // Hoạt Động lặp: reset tiến độ theo chu kỳ daily/weekly/monthly (mỗi 5 phút)
        scheduler.scheduleAtFixedRate(this::activityResetTask, 60, 300, TimeUnit.SECONDS);

        log.info("[EVENT] Scheduler started.");
    }


    /** Giảm nhu cầu con theo thời gian; bảo mẫu (nanny_until > NOW) tự chăm đầy. */
    private void childCareTask() {
        try (java.sql.Connection c = com.nexusisekai.database.DatabaseManager.getInstance().getConnection()) {
            // Con có bảo mẫu còn hạn → giữ nhu cầu đầy + happiness cao
            com.nexusisekai.database.SqlSafe.update(c,
                "UPDATE children SET hunger=100, thirst=100, cleanliness=100, happiness=LEAST(100,happiness+2) " +
                "WHERE nanny_until IS NOT NULL AND nanny_until > NOW()");
            // Con KHÔNG có bảo mẫu → nhu cầu giảm; nếu đói/khát/bẩn thì happiness giảm
            com.nexusisekai.database.SqlSafe.update(c,
                "UPDATE children SET " +
                "hunger=GREATEST(0,hunger-5), thirst=GREATEST(0,thirst-5), cleanliness=GREATEST(0,cleanliness-4), " +
                "happiness=GREATEST(0, happiness - CASE WHEN hunger<20 OR thirst<20 OR cleanliness<20 THEN 5 ELSE 0 END) " +
                "WHERE nanny_until IS NULL OR nanny_until <= NOW()");
        } catch (Exception e) {
            log.warn("childCareTask error: {}", e.getMessage());
        }
    }

    /**
     * World Boss: tự spawn theo chu kỳ (spawn_interval_min) và despawn khi hết giờ (active_until).
     * Khi spawn: reset HP, xoá bảng damage của lượt trước, thông báo toàn server.
     */
    private void worldBossTask() {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // 1. Despawn boss đã hết giờ mà chưa bị giết (boss thoát, không ai nhận thưởng kết liễu)
            com.nexusisekai.database.SqlSafe.update(c,
                "UPDATE world_bosses SET is_alive=0 WHERE is_alive=1 AND active_until IS NOT NULL AND active_until < NOW()");

            // 2. Spawn boss đủ điều kiện: chưa sống + (chưa spawn lần nào HOẶC đã qua chu kỳ)
            java.util.List<java.util.Map<String,Object>> ready = com.nexusisekai.database.SqlSafe.query(c,
                "SELECT id,name,map_id,hp,duration_min FROM world_bosses " +
                "WHERE is_alive=0 AND (last_spawn_at IS NULL OR last_spawn_at <= DATE_SUB(NOW(), INTERVAL spawn_interval_min MINUTE))");
            for (java.util.Map<String,Object> bo : ready) {
                int id = ((Number) bo.get("id")).intValue();
                int mapId = ((Number) bo.get("map_id")).intValue();
                long hp = ((Number) bo.get("hp")).longValue();
                int dur = ((Number) bo.get("duration_min")).intValue();
                // đánh dấu sống + đặt HP + hạn giờ (atomic, tránh spawn trùng nếu 2 tick chạy gần nhau)
                int spawned = com.nexusisekai.database.SqlSafe.update(c,
                    "UPDATE world_bosses SET is_alive=1, current_hp=?, active_until=DATE_ADD(NOW(), INTERVAL ? MINUTE), last_spawn_at=NOW(), last_killer_name=NULL " +
                    "WHERE id=? AND is_alive=0", hp, dur, id);
                if (spawned == 0) continue;
                // xoá damage lượt trước (spawn_seq=0 dùng làm lượt hiện tại)
                com.nexusisekai.database.SqlSafe.update(c, "DELETE FROM world_boss_damage WHERE boss_id=?", id);
                // thông báo toàn server
                String name = (String) bo.get("name");
                io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer();
                byte[] nb = name.getBytes(StandardCharsets.UTF_8);
                buf.writeInt(id); buf.writeShort(nb.length); buf.writeBytes(nb);
                byte[] arr = new byte[buf.readableBytes()]; buf.readBytes(arr);
                world.getNetworkServer().broadcast(PacketOpcode.S2C_WORLDBOSS_SPAWN, arr);
                log.info("[WORLDBOSS] Spawn '{}' (id={}) map={} duration={}min", name, id, mapId, dur);
            }
        } catch (Exception e) {
            log.warn("worldBossTask error: {}", e.getMessage());
        }
    }

    /**
     * Guild War: scheduled→ongoing khi tới start_at; ongoing→ended khi tới end_at.
     * Khi kết thúc: xác định guild thắng theo điểm, trao thưởng cho thành viên guild thắng.
     */
    private void guildWarTask() {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // 1. scheduled → ongoing
            com.nexusisekai.database.SqlSafe.update(c,
                "UPDATE guild_wars SET status='ongoing' WHERE status='scheduled' AND start_at <= NOW()");

            // 2. ongoing → ended: lấy các trận vừa hết giờ để xử lý thưởng
            java.util.List<java.util.Map<String,Object>> finished = com.nexusisekai.database.SqlSafe.query(c,
                "SELECT id,guild_a,guild_b,score_a,score_b FROM guild_wars WHERE status='ongoing' AND end_at <= NOW()");
            for (java.util.Map<String,Object> w : finished) {
                int id = ((Number) w.get("id")).intValue();
                int ga = ((Number) w.get("guild_a")).intValue();
                int gb = ((Number) w.get("guild_b")).intValue();
                int sa = ((Number) w.get("score_a")).intValue();
                int sb = ((Number) w.get("score_b")).intValue();
                Integer winner = sa == sb ? null : (sa > sb ? ga : gb);
                // chốt trạng thái (atomic), chỉ xử lý nếu còn 'ongoing'
                int ended = com.nexusisekai.database.SqlSafe.update(c,
                    "UPDATE guild_wars SET status='ended', winner_guild=? WHERE id=? AND status='ongoing'",
                    winner, id);
                if (ended == 0) continue;
                if (winner != null) {
                    // thưởng thành viên guild thắng (vàng + kim cương)
                    com.nexusisekai.database.SqlSafe.update(c,
                        "UPDATE characters SET gold=gold+200000, diamond=diamond+50 WHERE guild_id=?", winner);
                    log.info("[GUILDWAR] War {} ket thuc — guild {} thang ({}-{})", id, winner, sa, sb);
                } else {
                    log.info("[GUILDWAR] War {} hoa ({}-{})", id, sa, sb);
                }
            }
        } catch (Exception e) {
            log.warn("guildWarTask error: {}", e.getMessage());
        }
    }

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
    }

    /** Reset tiến độ hoạt động lặp theo chu kỳ (daily/weekly/monthly). */
    private void activityResetTask() {
        try (Connection c = DatabaseManager.getConnection()) {
            // daily: reset khi sang ngày mới
            resetIfDue(c, "daily",   "last_reset_at IS NULL OR DATE(last_reset_at) < CURDATE()");
            // weekly: reset khi sang tuần mới (YEARWEEK)
            resetIfDue(c, "weekly",  "last_reset_at IS NULL OR YEARWEEK(last_reset_at,1) < YEARWEEK(NOW(),1)");
            // monthly: reset khi sang tháng mới
            resetIfDue(c, "monthly", "last_reset_at IS NULL OR DATE_FORMAT(last_reset_at,'%Y%m') < DATE_FORMAT(NOW(),'%Y%m')");
        } catch (Exception e) {
            log.warn("activityResetTask error: {}", e.getMessage());
        }
    }
    private void resetIfDue(Connection c, String period, String dueCond) throws Exception {
        java.util.List<java.util.Map<String,Object>> due = com.nexusisekai.database.SqlSafe.query(c,
            "SELECT id FROM activities WHERE reset_period=? AND (" + dueCond + ")", period);
        for (java.util.Map<String,Object> a : due) {
            int id = ((Number) a.get("id")).intValue();
            com.nexusisekai.database.SqlSafe.update(c, "DELETE FROM activity_progress WHERE activity_id=?", id);
            com.nexusisekai.database.SqlSafe.update(c, "UPDATE activities SET last_reset_at=NOW() WHERE id=?", id);
            log.info("[ACTIVITY] Reset {} hoat dong {}", period, id);
        }
    }

    /** Phát thưởng đua top cho các hoạt động ranking đã kết thúc mà chưa phát. */
    private void activityRankingTask() {
        try (Connection c = DatabaseManager.getConnection()) {
            java.util.List<java.util.Map<String,Object>> ended = com.nexusisekai.database.SqlSafe.query(c,
                "SELECT id FROM activities WHERE activity_type='ranking' AND rewards_distributed=0 " +
                "AND end_at IS NOT NULL AND end_at < NOW()");
            for (java.util.Map<String,Object> a : ended) {
                int id = ((Number) a.get("id")).intValue();
                // chốt cờ trước (atomic) để tránh phát trùng nếu 2 tick
                int marked = com.nexusisekai.database.SqlSafe.update(c,
                    "UPDATE activities SET rewards_distributed=1 WHERE id=? AND rewards_distributed=0", id);
                if (marked == 0) continue;
                com.nexusisekai.network.handler.ActivityHandler.distributeRankingRewards(c, id);
                log.info("[ACTIVITY] Phat thuong dua top hoat dong {}", id);
            }
        } catch (Exception e) {
            log.warn("activityRankingTask error: {}", e.getMessage());
        }
    }

    private void checkScheduledEvents() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT * FROM events WHERE is_active=1 AND start_time <= NOW() AND end_time >= NOW()");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int    eventId   = rs.getInt("id");
                String eventName = rs.getString("name");
                int    eventType = rs.getInt("event_type");
                handleActiveEvent(eventId, eventName, eventType, rs.getString("params_json"));
            }
        } catch (Exception e) {
            log.error("checkScheduledEvents error: {}", e.getMessage());
        }
    }

    private void handleActiveEvent(int id, String name, int type, String params) {
        // Broadcast thông báo sự kiện
        byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + 2 + nameBytes.length);
        buf.putInt(id);
        buf.put((byte) type);
        buf.putShort((short) nameBytes.length);
        buf.put(nameBytes);

        if (world.getNetworkServer() != null) {
            world.getNetworkServer().broadcast(PacketOpcode.S2C_EVENT_START, buf.array());
        }
    }

    private void pingAllClients() {
        if (world.getNetworkServer() != null) {
            world.getNetworkServer().broadcast(PacketOpcode.S2C_PING, new byte[]{});
        }
    }

    private void autosaveAllPlayers() {
        if (world.getNetworkServer() == null) return;
        int saved = 0;
        for (var session : world.getNetworkServer().getAllSessions()) {
            if (session.getPlayer() != null) {
                session.getPlayer().saveToDb();
                saved++;
            }
        }
        if (saved > 0) log.debug("[AUTOSAVE] Saved {} players.", saved);
    }

    /** Poll cross_server_relay để broadcast tin từ server khác */
    private void pollCrossServerRelay() {
        int myServerId = com.nexusisekai.game.server.ServerManager.getInstance().getCurrentServerId();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT id, sender_name, content_type, content FROM cross_server_relay " +
                 "WHERE source_server != ? AND (sent_to IS NULL OR sent_to NOT LIKE ?) " +
                 "AND created_at >= DATE_SUB(NOW(), INTERVAL 30 SECOND) " +
                 "ORDER BY created_at ASC LIMIT 20")) {
            ps.setInt(1, myServerId);
            ps.setString(2, "%\"" + myServerId + "\"%");
            var rs = ps.executeQuery();
            while (rs.next()) {
                long relayId    = rs.getLong("id");
                String sender   = rs.getString("sender_name");
                String content  = rs.getString("content");

                // Broadcast sang channel cross trên server này
                byte[] senderBytes  = sender.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                byte[] contentBytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                io.netty.buffer.ByteBuf buf = io.netty.buffer.Unpooled.buffer(
                    8 + senderBytes.length + contentBytes.length);
                buf.writeShort(com.nexusisekai.network.PacketOpcode.S2C_CHAT);
                buf.writeByte(5);  // CH_CROSS
                buf.writeByte(0);  // CT_TEXT
                buf.writeShort(senderBytes.length); buf.writeBytes(senderBytes);
                buf.writeShort(contentBytes.length); buf.writeBytes(contentBytes);

                if (world.getNetworkServer() != null)
                    world.getNetworkServer().broadcastAll(buf);

                // Mark as delivered to this server
                conn.prepareStatement(
                    "UPDATE cross_server_relay SET sent_to=JSON_ARRAY_APPEND(" +
                    "COALESCE(sent_to,'[]'),\"$\"," + myServerId + ") WHERE id=" + relayId)
                    .executeUpdate();
            }
        } catch (Exception e) { log.error("[RELAY] poll error: {}", e.getMessage()); }
    }
}
