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

    public void stop() {
        if (scheduler != null) scheduler.shutdownNow();
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
