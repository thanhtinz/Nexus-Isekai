package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.SessionRegistry;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;

/**
 * FurnitureHandler — tương tác nội thất trong nhà:
 * ngồi ghế, nằm giường, ăn (hồi HP), uống (hồi MP), tắm...
 */
public class FurnitureHandler {
    private static final Logger log = LoggerFactory.getLogger(FurnitureHandler.class);

    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }
    static void msg(GameSession s,String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_SERVER_MSG); writeStr(b,m); s.send(b); }

    /** Tương tác với 1 nội thất đã đặt trong nhà. Payload: [long furnitureInstanceId] */
    public static void handleInteract(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        long fid = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            // lấy nội thất + loại tương tác + hồi phục (chỉ trong nhà của mình/vợ chồng)
            var row = SqlSafe.queryOne(c,
                "SELECT hf.id, hf.occupied_by, fc.interaction_type, fc.interact_anim, fc.restore_hp, fc.restore_mp " +
                "FROM house_furniture hf JOIN furniture_catalog fc ON fc.id=hf.furniture_id WHERE hf.id=?", fid);
            if (row == null) { msg(s, "Khong tim thay vat dung"); return; }
            String type = (String)row.get("interaction_type");
            if ("none".equals(type)) { msg(s, "Vat dung nay khong tuong tac duoc"); return; }
            long occ = ((Number)row.get("occupied_by")).longValue();
            if (occ != 0 && occ != p.getCharId()) { msg(s, "Co nguoi dang dung"); return; }

            // đánh dấu đang dùng + hồi HP/MP
            SqlSafe.update(c, "UPDATE house_furniture SET occupied_by=? WHERE id=?", p.getCharId(), fid);
            int rhp = ((Number)row.get("restore_hp")).intValue();
            int rmp = ((Number)row.get("restore_mp")).intValue();
            if (rhp > 0) p.heal(rhp);
            if (rmp > 0) p.restoreMp(rmp);

            String anim = (String)row.get("interact_anim");
            // broadcast cho người trong cùng instance thấy (ngồi/nằm/ăn...)
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_FURNITURE_INTERACT);
            pkt.writeLong(p.getCharId()); pkt.writeLong(fid); writeStr(pkt, type); writeStr(pkt, anim);
            pkt.writeInt(p.getHp()); pkt.writeInt(p.getMp());
            broadcastToInstance(p, pkt);
        } catch (Exception e) { log.warn("furniture interact", e); msg(s, "Loi tuong tac"); }
    }

    /** Đứng dậy / ngừng dùng. */
    public static void handleStop(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            SqlSafe.update(c, "UPDATE house_furniture SET occupied_by=0 WHERE occupied_by=?", p.getCharId());
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_FURNITURE_STOP);
            pkt.writeLong(p.getCharId());
            broadcastToInstance(p, pkt);
        } catch (Exception e) { /* ignore */ }
    }

    /** Mua nội thất (trừ tiền) rồi thêm vào kho nhà. Payload: [int furnitureId] */
    public static void handleBuy(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        int furnitureId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var fc = SqlSafe.queryOne(c, "SELECT gold_price, diamond_price, name FROM furniture_catalog WHERE id=? AND is_active=1", furnitureId);
            if (fc == null) { msg(s, "Vat dung khong ton tai"); return; }
            int gold = ((Number)fc.get("gold_price")).intValue();
            int dia  = ((Number)fc.get("diamond_price")).intValue();
            if (gold > 0 && p.getGold() < gold) { msg(s, "Khong du vang"); return; }
            if (dia > 0) {
                var dRow = SqlSafe.queryOne(c, "SELECT diamond FROM characters WHERE id=?", p.getCharId());
                int curDia = dRow != null ? ((Number)dRow.get("diamond")).intValue() : 0;
                if (curDia < dia) { msg(s, "Khong du kim cuong"); return; }
                SqlSafe.update(c, "UPDATE characters SET diamond=diamond-? WHERE id=?", dia, p.getCharId());
            }
            if (gold > 0) { p.setGold(p.getGold() - gold); SqlSafe.update(c, "UPDATE characters SET gold=gold-? WHERE id=?", gold, p.getCharId()); }
            // thêm vào nhà (lấy house_id của char)
            var house = SqlSafe.queryOne(c, "SELECT id FROM houses WHERE char_id=?", p.getCharId());
            if (house == null) { msg(s, "Ban chua co nha"); return; }
            long houseId = ((Number)house.get("id")).longValue();
            SqlSafe.insert(c, "INSERT INTO house_furniture (house_id, furniture_id) VALUES (?,?)", houseId, furnitureId);
            ByteBuf pkt = Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_FURNITURE_BUY);
            pkt.writeInt(furnitureId); pkt.writeBoolean(true); s.send(pkt);
            msg(s, "Da mua " + fc.get("name"));
        } catch (Exception e) { log.warn("furniture buy", e); msg(s, "Loi mua vat dung"); }
    }

    private static void broadcastToInstance(Player p, ByteBuf pkt) {
        var zm = WorldManager.getInstance().getZoneManager();
        var zone = zm.getZoneOf(p);
        if (zone != null) for (Player other : zone.getPlayers()) {
            var sess = SessionRegistry.getByCharId(other.getCharId());
            if (sess != null) sess.send(pkt.copy());
        }
        pkt.release();
    }
}
