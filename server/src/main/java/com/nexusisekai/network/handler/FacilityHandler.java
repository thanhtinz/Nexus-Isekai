package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.world.FacilityManager;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.game.social.SocialManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * FacilityHandler — vào/ra map facility (guild/lễ đường/nhà/nông trại/đấu trường/minigame).
 * Người chơi đi tới cổng → C2S_ENTER_FACILITY(category) → server resolve instance riêng → vào.
 */
public class FacilityHandler {
    private static final Logger log = LoggerFactory.getLogger(FacilityHandler.class);

    static void writeStr(ByteBuf b, String s) {
        byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d);
    }
    static String readStr(ByteBuf b){ int n=b.readShort(); byte[] d=new byte[n]; b.readBytes(d); return new String(d,StandardCharsets.UTF_8); }
    static void msg(GameSession s,String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_SERVER_MSG); writeStr(b,m); s.send(b); }

    /** Liệt kê cổng facility trên map hiện tại. */
    public static void handleFacilityPortals(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            List<Map<String,Object>> portals = SqlSafe.query(c,
                "SELECT id, pos_x, pos_y, facility_category, label, level_req, icon_id " +
                "FROM map_portals WHERE map_id=? AND portal_type='facility'", p.getMapId());
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_FACILITY_PORTALS);
            pkt.writeShort(portals.size());
            for (var po : portals) {
                pkt.writeInt(((Number)po.get("id")).intValue());
                pkt.writeFloat(((Number)po.get("pos_x")).floatValue());
                pkt.writeFloat(((Number)po.get("pos_y")).floatValue());
                writeStr(pkt, (String)po.get("facility_category"));
                writeStr(pkt, (String)po.get("label"));
                pkt.writeInt(((Number)po.get("level_req")).intValue());
                pkt.writeInt(((Number)po.get("icon_id")).intValue());
            }
            s.send(pkt);
        } catch (Exception e) { log.warn("facility portals", e); msg(s, "Loi danh sach cong"); }
    }

    /** Vào facility theo category. */
    public static void handleEnterFacility(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        String category = readStr(buf);
        FacilityManager fm = FacilityManager.getInstance();
        var map = fm.getFacilityMap(category);
        if (map == null) { msg(s, "Khu vuc khong ton tai"); return; }

        int facilityMapId = ((Number)map.get("id")).intValue();
        String scope = (String)map.get("instance_scope");

        // Điều kiện riêng từng loại
        if (category.equals("guild") && p.getGuildId() <= 0) { msg(s, "Ban chua co bang hoi"); return; }

        long ownerId = fm.resolveOwnerId(scope, p.getCharId(), p.getGuildId(), 0L /*partyId*/);
        String ownerType = switch (scope) {
            case "guild" -> "guild"; case "party" -> "party"; case "room" -> "room"; case "personal" -> "char"; default -> "global";
        };

        // NHÀ CHUNG: nếu vào "housing" mà đã kết hôn → instance theo marriage
        // → cả hai vợ chồng vào CÙNG một căn nhà.
        if (category.equals("home") || category.equals("house_interior")) {
            try {
                var marriage = SocialManager.getInstance().getMarriage(p.getCharId());
                if (marriage != null && "married".equals(marriage.status)) {
                    ownerType = "marriage";
                    ownerId = marriage.id;
                    // đồng bộ spouse_id trên bản ghi nhà (cả 2 chiều)
                    long spouse = (marriage.charIdA == p.getCharId()) ? marriage.charIdB : marriage.charIdA;
                    try (java.sql.Connection cc = com.nexusisekai.database.DatabaseManager.getInstance().getConnection()) {
                        com.nexusisekai.database.SqlSafe.update(cc,
                            "UPDATE houses SET spouse_id=? WHERE char_id IN (?,?)", spouse, p.getCharId(), spouse);
                    }
                }
            } catch (Exception ex) { log.warn("housing marriage check", ex); }
        }

        long instanceId = scope.equals("static") ? 0 : fm.getOrCreateInstance(facilityMapId, ownerType, ownerId);

        // Rời zone cũ → vào facility
        var zm = WorldManager.getInstance().getZoneManager();
        zm.removePlayer(p);
        p.setInstanceId(instanceId);
        p.setMapId(facilityMapId);
        p.setX(50); p.setY(50);
        zm.addPlayerToInstance(p, facilityMapId, instanceId);

        ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_FACILITY_ENTER);
        pkt.writeInt(facilityMapId);
        pkt.writeLong(instanceId);
        writeStr(pkt, (String)map.get("name"));
        writeStr(pkt, (String)map.get("file_name"));
        writeStr(pkt, category);
        s.send(pkt);
        log.info("{} vao facility {} (map {} instance {})", p.getName(), category, facilityMapId, instanceId);
    }

    /** Rời facility → quay về map gốc. */
    public static void handleLeaveFacility(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var map = SqlSafe.queryOne(c, "SELECT return_map_id, return_x, return_y FROM maps WHERE id=?", p.getMapId());
            int retMap = map != null ? ((Number)map.get("return_map_id")).intValue() : 1;
            float rx = map != null ? ((Number)map.get("return_x")).floatValue() : 100;
            float ry = map != null ? ((Number)map.get("return_y")).floatValue() : 100;
            if (retMap <= 0) retMap = 1;

            var zm = WorldManager.getInstance().getZoneManager();
            zm.removePlayer(p);
            p.setInstanceId(0);
            p.setMapId(retMap);
            p.setX(rx); p.setY(ry);
            zm.addPlayer(p);

            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_FACILITY_LEFT);
            pkt.writeInt(retMap); pkt.writeFloat(rx); pkt.writeFloat(ry); s.send(pkt);
        } catch (Exception e) { msg(s, "Loi roi khu vuc"); }
    }

    /** Tương tác nội thất: ngồi ghế / nằm giường / ăn / uống. */
    public static void handleFurnitureInteract(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        long furnitureInstanceId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // Lấy nội thất + định nghĩa tương tác (phải thuộc nhà người chơi đang ở)
            var f = SqlSafe.queryOne(c,
                "SELECT hf.id, hf.pos_x, hf.pos_y, fc.interaction_type, fc.animation_state, fc.interaction_effect, fc.is_consumable, fc.name " +
                "FROM house_furniture hf JOIN furniture_catalog fc ON fc.id=hf.furniture_id " +
                "JOIN houses h ON h.id=hf.house_id " +
                "WHERE hf.id=? AND (h.char_id=? OR h.spouse_id=?)",
                furnitureInstanceId, p.getCharId(), p.getCharId());
            if (f == null) { msg(s, "Khong tim thay vat dung"); return; }
            String type = (String)f.get("interaction_type");
            if (type == null || type.equals("none")) { msg(s, "Vat dung nay khong tuong tac duoc"); return; }

            String anim = (String)f.get("animation_state");
            String effectJson = (String)f.get("interaction_effect");
            boolean consumable = ((Number)f.get("is_consumable")).intValue() == 1;

            // Áp dụng hiệu ứng hồi phục/buff (parse JSON đơn giản)
            applyInteractionEffect(p, effectJson);

            // Phát animation cho người trong cùng instance (ngồi/nằm/ăn/uống)
            var zm = WorldManager.getInstance().getZoneManager();
            ByteBuf bc = Unpooled.buffer(); bc.writeShort(PacketOpcode.S2C_FURNITURE_INTERACT);
            bc.writeLong(p.getCharId()); writeStr(bc, anim); bc.writeLong(furnitureInstanceId);
            broadcastToInstance(zm, p, bc);

            // ăn/uống tốn vật phẩm 1 lần dùng (consumable) → có thể trừ ở đây
            if (consumable) { /* TODO: trừ lượt/đồ ăn nếu thiết kế hữu hạn */ }
            log.debug("{} tuong tac {} ({})", p.getName(), f.get("name"), type);
        } catch (Exception e) { log.warn("furniture interact", e); msg(s, "Loi tuong tac"); }
    }

    /** Dừng tương tác (đứng dậy khỏi ghế/giường). */
    public static void handleFurnitureStop(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        var zm = WorldManager.getInstance().getZoneManager();
        ByteBuf bc = Unpooled.buffer(); bc.writeShort(PacketOpcode.S2C_FURNITURE_STOP);
        bc.writeLong(p.getCharId());
        broadcastToInstance(zm, p, bc);
    }

    /** Hồi HP/MP + buff từ interaction_effect JSON. */
    private static void applyInteractionEffect(Player p, String json) {
        if (json == null || json.isBlank()) return;
        try {
            // parse JSON tối giản (không phụ thuộc lib): tìm "hp_regen":N, "mp_regen":N
            int hp = extractInt(json, "hp_regen");
            int mp = extractInt(json, "mp_regen");
            if (hp > 0) p.setHp(Math.min(p.getMaxHp(), p.getHp() + hp));
            if (mp > 0) p.setMp(Math.min(p.getMaxMp(), p.getMp() + mp));
            // buff name + duration → có thể đẩy vào hệ buff nếu cần
        } catch (Exception ignored) {}
    }

    private static int extractInt(String json, String key) {
        int i = json.indexOf("\"" + key + "\"");
        if (i < 0) return 0;
        int colon = json.indexOf(':', i);
        if (colon < 0) return 0;
        int j = colon + 1;
        StringBuilder sb = new StringBuilder();
        while (j < json.length() && (Character.isDigit(json.charAt(j)) || json.charAt(j)=='-')) { sb.append(json.charAt(j)); j++; }
        return sb.length() > 0 ? Integer.parseInt(sb.toString()) : 0;
    }

    /** Broadcast tới mọi người trong cùng instance (hoặc map nếu không instance). */
    private static void broadcastToInstance(com.nexusisekai.game.world.ZoneManager zm, Player p, ByteBuf pkt) {
        var zone = zm.getZoneOf(p);
        if (zone == null) { pkt.release(); return; }
        for (Player other : zone.getPlayers()) {
            var sess = com.nexusisekai.network.SessionRegistry.getByCharId(other.getCharId());
            if (sess != null) sess.send(pkt.copy());
        }
        pkt.release();
    }
}
