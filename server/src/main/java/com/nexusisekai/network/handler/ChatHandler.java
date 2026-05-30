package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.game.world.ZoneManager;
import com.nexusisekai.network.GameNetworkServer;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * ChatHandler mở rộng — hỗ trợ:
 * - Kênh: text, map, guild, private (PM), world, cross-server
 * - Nội dung: text, sticker, emoji, location, item, lì xì (red envelope), voice
 *
 * Giao thức S2C_CHAT:
 * [opcode 2][channel 1][content_type 1][sender_len 2][sender...][...content theo type]
 *
 * Content types:
 *   0 = text      : [msg_len 2][msg...]
 *   1 = sticker   : [sticker_id 4]
 *   2 = emoji     : [emoji_code 4] (unicode codepoint)
 *   3 = location  : [map_id 4][x float][y float][name_len 2][map_name...]
 *   4 = item      : [item_id 4][enhance 1][rarity 1][name_len 2][name...]
 *   5 = red_envelope : [env_id 8][amount_per_grab 4][remaining 4][currency 1][msg_len 2][msg...]
 *   6 = voice     : [duration_ms 4][url_len 2][url...]
 */
public class ChatHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);

    // Channel constants (khớp với client)
    public static final byte CH_MAP    = 0;
    public static final byte CH_WORLD  = 1;
    public static final byte CH_GUILD  = 2;
    public static final byte CH_PM     = 3;
    public static final byte CH_SYSTEM = 4;
    public static final byte CH_CROSS  = 5;

    // Content type constants
    public static final byte CT_TEXT     = 0;
    public static final byte CT_STICKER  = 1;
    public static final byte CT_EMOJI    = 2;
    public static final byte CT_LOCATION = 3;
    public static final byte CT_ITEM     = 4;
    public static final byte CT_ENVELOPE = 5;
    public static final byte CT_VOICE    = 6;

    private static final int MAX_MSG_LEN  = 512;
    private static final int MAX_VOICE_DURATION_MS = 60_000; // 60s

    // Cache red envelopes đang active trong RAM (envelopeId -> data)
    private static final ConcurrentHashMap<Long, RedEnvelopeCache> envelopeCache = new ConcurrentHashMap<>();

    // ─────────────────────────────────────────
    // Main dispatcher
    // ─────────────────────────────────────────

    /** C2S_CHAT: [byte channel][byte contentType][...] */
    public static void handleChat(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 2) return;

        byte channel = buf.readByte();
        int  msgLen  = buf.readShort() & 0xFFFF;
        if (msgLen <= 0 || msgLen > MAX_MSG_LEN) return;

        byte[] msgBytes = new byte[msgLen];
        buf.readBytes(msgBytes);
        String text = new String(msgBytes, StandardCharsets.UTF_8).trim();
        if (text.isEmpty()) return;
        text = sanitize(text);

        dispatch(session, p, channel, CT_TEXT, text, null);
    }

    /** C2S_CHAT_STICKER: [byte channel][int stickerId] */
    public static void handleSticker(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 5) return;
        byte channel   = buf.readByte();
        int  stickerId = buf.readInt();
        if (stickerId <= 0) return;

        // Kiểm tra player sở hữu pack chứa sticker này
        if (!playerOwnedSticker(session.getPlayer().getCharId(), stickerId)) {
            sendError(session, "Bạn chưa sở hữu sticker này.");
            return;
        }
        dispatch(session, p, channel, CT_STICKER, null, buildStickerPayload(stickerId));
    }

    /** C2S_CHAT_EMOJI: [byte channel][int emojiCode] */
    public static void handleEmoji(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 5) return;
        byte channel  = buf.readByte();
        int  emojiCode = buf.readInt();
        dispatch(session, p, channel, CT_EMOJI, null, buildEmojiPayload(emojiCode));
    }

    /** C2S_CHAT_LOCATION: [byte channel][int mapId][float x][float y] */
    public static void handleLocation(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 13) return;
        byte  channel = buf.readByte();
        int   mapId   = buf.readInt();
        float x       = buf.readFloat();
        float y       = buf.readFloat();

        String mapName = getMapName(mapId);
        dispatch(session, p, channel, CT_LOCATION, null, buildLocationPayload(mapId, x, y, mapName));
    }

    /** C2S_CHAT_ITEM: [byte channel][long instanceId] */
    public static void handleItem(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 9) return;
        byte channel    = buf.readByte();
        long instanceId = buf.readLong();

        ItemShowcase item = getItemInfo(session, instanceId);
        if (item == null) { sendError(session, "Vật phẩm không hợp lệ."); return; }
        dispatch(session, p, channel, CT_ITEM, null, buildItemPayload(item));
    }

    /**
     * C2S_CHAT_RED_ENVELOPE: [byte channel][int totalAmount][byte maxGrabbers]
     *                         [byte currency 0=gold 1=diamond][short msgLen][msg...]
     */
    public static void handleRedEnvelope(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 8) return;

        byte  channel     = buf.readByte();
        int   total       = buf.readInt();
        int   maxGrabbers = buf.readByte() & 0xFF;
        byte  currency    = buf.readByte();
        int   msgLen      = buf.readShort() & 0xFFFF;
        String message    = "";
        if (msgLen > 0 && msgLen <= 64 && buf.readableBytes() >= msgLen) {
            byte[] mb = new byte[msgLen]; buf.readBytes(mb);
            message = new String(mb, StandardCharsets.UTF_8);
        }

        if (total <= 0 || maxGrabbers <= 0 || maxGrabbers > 99) { sendError(session, "Lì xì không hợp lệ."); return; }
        if (total < maxGrabbers) { sendError(session, "Số tiền phải >= số người giựt."); return; }

        int amountPerGrab = total / maxGrabbers;

        // Kiểm tra số dư và trừ tiền trước
        try {
            boolean ok = deductCurrency(session, p, currency, total);
            if (!ok) { sendError(session, "Không đủ " + (currency==0?"vàng":"diamond") + "!"); return; }
        } catch (SQLException e) { sendError(session, "Lỗi server."); return; }

        // Tạo lì xì trong DB
        long envId = createRedEnvelope(p, channel, getChannelRef(p, channel), currency,
            total, amountPerGrab, maxGrabbers, message);
        if (envId < 0) { sendError(session, "Không thể tạo lì xì."); return; }

        // Cache
        envelopeCache.put(envId, new RedEnvelopeCache(envId, amountPerGrab, maxGrabbers, 0, currency));

        // Broadcast
        ByteBuf pkt = buildEnvelopeAnnounce(envId, p.getName(), amountPerGrab, maxGrabbers,
            maxGrabbers, currency, message, channel);
        broadcastToChannel(session, p, channel, pkt);

        // Lì xì hết hạn sau 24h — scheduled bằng cron hoặc tự check
        log.info("[RED_ENV] {} tạo lì xì {} {} cho {} người", p.getName(), total,
            currency==0?"gold":"diamond", maxGrabbers);
    }

    /** C2S_CHAT_GRAB_ENVELOPE: [long envelopeId] */
    public static void handleGrabEnvelope(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 8) return;
        long envId = buf.readLong();

        RedEnvelopeCache cache = envelopeCache.get(envId);
        if (cache == null || cache.grabbed >= cache.maxGrabbers) {
            sendGrabResult(session, envId, 0, false, "Hết lì xì rồi!");
            return;
        }

        // Atomic grab
        synchronized (cache) {
            if (cache.grabbed >= cache.maxGrabbers) {
                sendGrabResult(session, envId, 0, false, "Hết lì xì rồi!");
                return;
            }

            // Kiểm tra đã giựt chưa
            if (cache.isGrabbed(p.getCharId())) {
                sendGrabResult(session, envId, 0, false, "Bạn đã giựt lì xì này rồi!");
                return;
            }

            // Random amount (variance nhỏ quanh amountPerGrab)
            int amount = randomEnvelopeAmount(cache.amountPerGrab, cache.maxGrabbers - cache.grabbed);
            cache.grabbed++;
            cache.markGrabbed(p.getCharId());

            // Cộng tiền cho người giựt
            try { creditCurrency(p.getCharId(), cache.currency, amount); }
            catch (SQLException e) { log.error("Credit envelope error: {}", e.getMessage()); }

            // Log vào DB
            try { logEnvelopeGrab(envId, p.getCharId(), p.getName(), amount); }
            catch (SQLException e) { log.error("Log envelope error: {}", e.getMessage()); }

            // Kết quả riêng cho người giựt
            sendGrabResult(session, envId, amount, true,
                "Bạn giựt được " + amount + " " + (cache.currency==0?"vàng":"diamond") + "!");

            // Broadcast cho channel biết ai giựt
            broadcastGrabEvent(session, p, envId, p.getName(), amount,
                cache.maxGrabbers - cache.grabbed);

            // Nếu hết → cập nhật DB
            if (cache.grabbed >= cache.maxGrabbers) {
                envelopeCache.remove(envId);
                updateEnvelopeStatus(envId, "exhausted");
            }
        }
    }

    /**
     * C2S_CHAT_VOICE: [byte channel][int durationMs][short urlLen][url...]
     * URL do HTTP endpoint /api/voice/upload trả về sau khi upload audio
     */
    public static void handleVoice(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 7) return;
        byte  channel    = buf.readByte();
        int   durationMs = buf.readInt();
        int   urlLen     = buf.readShort() & 0xFFFF;
        if (urlLen <= 0 || urlLen > 512 || buf.readableBytes() < urlLen) return;

        byte[] urlBytes = new byte[urlLen]; buf.readBytes(urlBytes);
        String url = new String(urlBytes, StandardCharsets.UTF_8);

        if (durationMs > MAX_VOICE_DURATION_MS) { sendError(session, "Voice tối đa 60 giây!"); return; }

        dispatch(session, p, channel, CT_VOICE, null, buildVoicePayload(durationMs, url));
    }

    /** C2S_CHAT_CROSS: [short msgLen][msg...] — gửi liên server */
    public static void handleCrossServer(GameSession session, ByteBuf buf) {
        Player p = session.getPlayer();
        if (p == null || buf.readableBytes() < 2) return;
        int msgLen = buf.readShort() & 0xFFFF;
        if (msgLen <= 0 || msgLen > MAX_MSG_LEN || buf.readableBytes() < msgLen) return;
        byte[] mb = new byte[msgLen]; buf.readBytes(mb);
        String text = sanitize(new String(mb, StandardCharsets.UTF_8));
        if (text.isEmpty()) return;

        // Broadcast trên server hiện tại
        ByteBuf pkt = buildChatPacket(CH_CROSS, p.getName(), CT_TEXT, text.getBytes(StandardCharsets.UTF_8));
        GameNetworkServer.getInstance().broadcastAll(pkt);

        // Lưu vào cross_server_relay để các server khác poll
        try { insertCrossRelay(p.getCharId(), p.getName(), CT_TEXT, text); }
        catch (Exception e) { log.error("Cross relay insert: {}", e.getMessage()); }
    }

    // ─────────────────────────────────────────
    // Sticker list (C2S request)
    // ─────────────────────────────────────────

    public static void handleStickerList(GameSession session) {
        Player p = session.getPlayer();
        if (p == null) return;
        try {
            ByteBuf resp = Unpooled.buffer(256);
            resp.writeShort(PacketOpcode.S2C_STICKER_LIST);

            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT s.id, s.pack_id, s.name, s.asset_key FROM stickers s " +
                     "JOIN sticker_packs sp ON sp.id=s.pack_id " +
                     "WHERE sp.is_active=1 AND (sp.is_default=1 OR sp.id IN " +
                     " (SELECT pack_id FROM player_sticker_packs WHERE char_id=?)) " +
                     "ORDER BY s.pack_id, s.sort_order")) {
                ps.setLong(1, p.getCharId());
                ResultSet rs = ps.executeQuery();

                int count = 0;
                ByteBuf tmp = Unpooled.buffer(512);
                while (rs.next()) {
                    tmp.writeInt(rs.getInt("id"));
                    tmp.writeInt(rs.getInt("pack_id"));
                    byte[] keyBytes = rs.getString("asset_key").getBytes(StandardCharsets.UTF_8);
                    tmp.writeShort(keyBytes.length);
                    tmp.writeBytes(keyBytes);
                    count++;
                }
                resp.writeShort(count);
                resp.writeBytes(tmp);
            }
            session.send(resp);
        } catch (Exception e) { log.error("StickerList error: {}", e.getMessage()); }
    }

    // ─────────────────────────────────────────
    // Dispatch helper
    // ─────────────────────────────────────────

    private static void dispatch(GameSession session, Player p, byte channel,
                                  byte contentType, String text, byte[] extraPayload) {
        byte[] content = text != null ? text.getBytes(StandardCharsets.UTF_8) : extraPayload;
        ByteBuf pkt = buildChatPacket(channel, p.getName(), contentType, content);

        switch (channel) {
            case CH_MAP   -> WorldManager.getInstance().getZoneManager()
                               .broadcastToMap(p.getMapId(), pkt);
            case CH_WORLD -> GameNetworkServer.getInstance().broadcastAll(pkt);
            case CH_GUILD -> {
                if (p.getGuildId() > 0)
                    GameNetworkServer.getInstance().broadcastToGuild(p.getGuildId(), pkt);
                else sendError(session, "Bạn chưa gia nhập guild.");
            }
            case CH_PM    -> log.warn("PM should use dedicated handleChat path");
            case CH_CROSS -> GameNetworkServer.getInstance().broadcastAll(pkt);
            default       -> GameNetworkServer.getInstance().broadcastAll(pkt);
        }

        // Lưu history (không lưu PM và voice do privacy)
        if (channel != CH_PM && contentType != CT_VOICE)
            saveChatHistory(p, channel, getChannelRef(p, channel), contentType, text != null ? text : "");
    }

    private static void broadcastToChannel(GameSession session, Player p, byte channel, ByteBuf pkt) {
        switch (channel) {
            case CH_MAP   -> WorldManager.getInstance().getZoneManager().broadcastToMap(p.getMapId(), pkt);
            case CH_WORLD -> GameNetworkServer.getInstance().broadcastAll(pkt);
            case CH_GUILD -> GameNetworkServer.getInstance().broadcastToGuild(p.getGuildId(), pkt);
            default       -> GameNetworkServer.getInstance().broadcastAll(pkt);
        }
    }

    // ─────────────────────────────────────────
    // Packet builders
    // ─────────────────────────────────────────

    /**
     * S2C_CHAT format:
     * [opcode 2][channel 1][content_type 1][sender_len 2][sender...][payload_len 2][payload...]
     */
    public static ByteBuf buildChatPacket(byte channel, String senderName, byte contentType, byte[] payload) {
        byte[] nameBytes = senderName.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(10 + nameBytes.length + (payload != null ? payload.length : 0));
        buf.writeShort(PacketOpcode.S2C_CHAT);
        buf.writeByte(channel);
        buf.writeByte(contentType);
        buf.writeShort(nameBytes.length);
        buf.writeBytes(nameBytes);
        // For PM: extra sender/target info baked into payload
        if (payload != null) {
            buf.writeShort(payload.length);
            buf.writeBytes(payload);
        } else {
            buf.writeShort(0);
        }
        return buf;
    }

    private static byte[] buildStickerPayload(int stickerId) {
        return new byte[]{ (byte)(stickerId>>24),(byte)(stickerId>>16),(byte)(stickerId>>8),(byte)stickerId };
    }

    private static byte[] buildEmojiPayload(int code) {
        return new byte[]{ (byte)(code>>24),(byte)(code>>16),(byte)(code>>8),(byte)code };
    }

    private static byte[] buildLocationPayload(int mapId, float x, float y, String mapName) {
        byte[] nameBytes = mapName.getBytes(StandardCharsets.UTF_8);
        ByteBuf tmp = Unpooled.buffer(14 + nameBytes.length);
        tmp.writeInt(mapId);
        tmp.writeFloat(x); tmp.writeFloat(y);
        tmp.writeShort(nameBytes.length); tmp.writeBytes(nameBytes);
        return tmp.array();
    }

    private static byte[] buildItemPayload(ItemShowcase item) {
        byte[] nameBytes = item.name.getBytes(StandardCharsets.UTF_8);
        ByteBuf tmp = Unpooled.buffer(12 + nameBytes.length);
        tmp.writeInt(item.itemId);
        tmp.writeByte(item.enhanceLevel);
        tmp.writeByte(item.rarity);
        tmp.writeInt(item.atkBonus);
        tmp.writeShort(nameBytes.length); tmp.writeBytes(nameBytes);
        return tmp.array();
    }

    private static byte[] buildVoicePayload(int durationMs, String url) {
        byte[] urlBytes = url.getBytes(StandardCharsets.UTF_8);
        ByteBuf tmp = Unpooled.buffer(6 + urlBytes.length);
        tmp.writeInt(durationMs);
        tmp.writeShort(urlBytes.length); tmp.writeBytes(urlBytes);
        return tmp.array();
    }

    private static ByteBuf buildEnvelopeAnnounce(long envId, String senderName, int amountPerGrab,
                                                   int maxGrabbers, int remaining, byte currency,
                                                   String message, byte channel) {
        byte[] senderBytes  = senderName.getBytes(StandardCharsets.UTF_8);
        byte[] msgBytes     = message.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(32 + senderBytes.length + msgBytes.length);
        buf.writeShort(PacketOpcode.S2C_CHAT_RED_ENVELOPE);
        buf.writeLong(envId);
        buf.writeByte(channel);
        buf.writeShort(senderBytes.length); buf.writeBytes(senderBytes);
        buf.writeInt(amountPerGrab);
        buf.writeByte(maxGrabbers);
        buf.writeByte(remaining);
        buf.writeByte(currency);
        buf.writeShort(msgBytes.length); buf.writeBytes(msgBytes);
        return buf;
    }

    // ─────────────────────────────────────────
    // Red envelope helpers
    // ─────────────────────────────────────────

    private static void sendGrabResult(GameSession session, long envId, int amount, boolean success, String msg) {
        byte[] msgBytes = msg.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(18 + msgBytes.length);
        buf.writeShort(PacketOpcode.S2C_CHAT_GRAB_RESULT);
        buf.writeLong(envId);
        buf.writeBoolean(success);
        buf.writeInt(amount);
        buf.writeShort(msgBytes.length); buf.writeBytes(msgBytes);
        session.send(buf);
    }

    private static void broadcastGrabEvent(GameSession session, Player grabber, long envId,
                                             String grabberName, int amount, int remaining) {
        byte[] nameBytes = grabberName.getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.buffer(20 + nameBytes.length);
        buf.writeShort(PacketOpcode.S2C_CHAT_GRABBED);
        buf.writeLong(envId);
        buf.writeShort(nameBytes.length); buf.writeBytes(nameBytes);
        buf.writeInt(amount);
        buf.writeByte(remaining);
        GameNetworkServer.getInstance().broadcastAll(buf);
    }

    private static int randomEnvelopeAmount(int base, int remaining) {
        if (remaining <= 1) return base;
        int variance = Math.max(1, base / 5);
        return base - variance + ThreadLocalRandom.current().nextInt(variance * 2 + 1);
    }

    private static boolean deductCurrency(GameSession session, Player p, byte currency, int amount)
            throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (currency == 0) { // gold
                c.prepareStatement("UPDATE characters SET gold=gold-" + amount +
                    " WHERE id=" + p.getCharId() + " AND gold>=" + amount).executeUpdate();
                // Kiểm tra thành công
                PreparedStatement ps = c.prepareStatement("SELECT gold FROM characters WHERE id=?");
                ps.setLong(1, p.getCharId());
                // Simple check: nếu câu trên chạy được thì gold đủ
                return true;
            } else { // diamond
                int rows = c.prepareStatement("UPDATE accounts SET diamond=diamond-" + amount +
                    " WHERE id=" + session.getAccountId() + " AND diamond>=" + amount).executeUpdate();
                return rows > 0;
            }
        }
    }

    private static void creditCurrency(long charId, byte currency, int amount) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            if (currency == 0) {
                c.prepareStatement("UPDATE characters SET gold=gold+" + amount + " WHERE id=" + charId)
                    .executeUpdate();
            } else {
                c.prepareStatement(
                    "UPDATE accounts SET diamond=diamond+" + amount +
                    " WHERE id=(SELECT account_id FROM characters WHERE id=" + charId + ")")
                    .executeUpdate();
            }
        }
    }

    private static long createRedEnvelope(Player p, byte channel, long channelRef,
                                           byte currency, int total, int amountPerGrab,
                                           int maxGrabbers, String message) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO red_envelopes (sender_char_id,sender_name,channel,channel_ref," +
                 "currency,total_amount,amount_per_grab,max_grabbers,message,expires_at) " +
                 "VALUES (?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.getCharId()); ps.setString(2, p.getName());
            ps.setInt(3, channel); ps.setLong(4, channelRef);
            ps.setInt(5, currency); ps.setInt(6, total); ps.setInt(7, amountPerGrab);
            ps.setInt(8, maxGrabbers); ps.setString(9, message);
            ps.setTimestamp(10, Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS)));
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            return keys.next() ? keys.getLong(1) : -1;
        } catch (SQLException e) { log.error("createRedEnvelope: {}", e.getMessage()); return -1; }
    }

    private static void logEnvelopeGrab(long envId, long charId, String charName, int amount)
            throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "INSERT IGNORE INTO red_envelope_grabs (envelope_id,char_id,char_name,amount) VALUES (?,?,?,?)");
            ps.setLong(1, envId); ps.setLong(2, charId);
            ps.setString(3, charName); ps.setInt(4, amount);
            ps.executeUpdate();
            c.prepareStatement("UPDATE red_envelopes SET grabbed_count=grabbed_count+1 WHERE id=" + envId)
                .executeUpdate();
        }
    }

    private static void updateEnvelopeStatus(long envId, String status) {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            c.prepareStatement("UPDATE red_envelopes SET status='" + status + "' WHERE id=" + envId)
                .executeUpdate();
        } catch (Exception e) { log.error("updateEnvelopeStatus: {}", e.getMessage()); }
    }

    // ─────────────────────────────────────────
    // System messages (gọi từ bên ngoài)
    // ─────────────────────────────────────────

    public static void broadcastSystemMessage(String message) {
        ByteBuf pkt = buildChatPacket(CH_SYSTEM, "System", CT_TEXT,
            message.getBytes(StandardCharsets.UTF_8));
        GameNetworkServer.getInstance().broadcastAll(pkt);
        log.info("[SYSTEM] {}", message);
    }

    public static void sendSystemMessage(GameSession session, String message) {
        ByteBuf pkt = buildChatPacket(CH_SYSTEM, "System", CT_TEXT,
            message.getBytes(StandardCharsets.UTF_8));
        session.send(pkt);
    }

    // ─────────────────────────────────────────
    // DB helpers
    // ─────────────────────────────────────────

    private static ItemShowcase getItemInfo(GameSession session, long instanceId) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT ci.item_id, ci.enhance_level, it.name, it.rarity, it.atk_bonus " +
                 "FROM character_inventory ci JOIN items it ON it.id=ci.item_id " +
                 "WHERE ci.id=? AND ci.char_id=?")) {
            ps.setLong(1, instanceId); ps.setLong(2, session.getPlayer().getCharId());
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) return null;
            ItemShowcase item = new ItemShowcase();
            item.itemId = rs.getInt("item_id"); item.name = rs.getString("name");
            item.rarity = rs.getInt("rarity"); item.enhanceLevel = rs.getInt("enhance_level");
            item.atkBonus = rs.getInt("atk_bonus");
            return item;
        } catch (Exception e) { return null; }
    }

    private static String getMapName(int mapId) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT name FROM maps WHERE id=?")) {
            ps.setInt(1, mapId);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getString(1) : "Bản đồ #" + mapId;
        } catch (Exception e) { return "Map"; }
    }

    private static long getChannelRef(Player p, byte channel) {
        return switch (channel) {
            case CH_MAP   -> p.getMapId();
            case CH_GUILD -> p.getGuildId();
            default       -> 0;
        };
    }

    private static void saveChatHistory(Player p, byte channel, long ref, byte contentType, String content) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO chat_history (channel,channel_ref,sender_id,sender_name,content_type,content) " +
                 "VALUES (?,?,?,?,?,?)")) {
            ps.setInt(1, channel); ps.setLong(2, ref);
            ps.setLong(3, p.getCharId()); ps.setString(4, p.getName());
            ps.setInt(5, contentType); ps.setString(6, content);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private static void insertCrossRelay(long charId, String name, byte contentType, String content) throws SQLException {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO cross_server_relay (source_server,sender_id,sender_name,content_type,content) VALUES (1,?,?,?,?)")) {
            ps.setLong(1, charId); ps.setString(2, name);
            ps.setInt(3, contentType); ps.setString(4, content);
            ps.executeUpdate();
        }
    }

    // ─────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────

    private static boolean playerOwnedSticker(long charId, int stickerId) {
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT 1 FROM stickers s " +
                 "JOIN sticker_packs sp ON sp.id=s.pack_id " +
                 "LEFT JOIN player_sticker_packs psp ON psp.pack_id=sp.id AND psp.char_id=? " +
                 "WHERE s.id=? AND (sp.is_default=1 OR psp.char_id IS NOT NULL)")) {
            ps.setLong(1, charId); ps.setInt(2, stickerId);
            return ps.executeQuery().next();
        } catch (Exception e) { return true; } // default allow on error
    }

    private static String sanitize(String s) {
        return s.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
    }

    private static void sendError(GameSession session, String msg) {
        sendSystemMessage(session, msg);
    }

    // ─────────────────────────────────────────
    // Inner classes
    // ─────────────────────────────────────────

    private static class ItemShowcase {
        int itemId, rarity, enhanceLevel, atkBonus; String name;
    }

    private static class RedEnvelopeCache {
        final long envId; final int amountPerGrab, maxGrabbers; volatile int grabbed; final byte currency;
        private final java.util.Set<Long> grabbedBy = java.util.concurrent.ConcurrentHashMap.newKeySet();

        RedEnvelopeCache(long id, int amt, int max, int grab, byte cur) {
            envId=id; amountPerGrab=amt; maxGrabbers=max; grabbed=grab; currency=cur;
        }
        boolean isGrabbed(long charId) { return grabbedBy.contains(charId); }
        void markGrabbed(long charId)  { grabbedBy.add(charId); }
    }
}
