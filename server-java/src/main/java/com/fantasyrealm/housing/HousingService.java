package com.fantasyrealm.housing;

import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Nhà ở & tài sản (RP, quản lý động qua DB):
 *  - Mua nhà (lô đất chưa có chủ) → sở hữu
 *  - Vào nhà (chủ + nếu mở khóa thì khách), khóa/mở cửa
 *  - Mua + đặt nội thất, lưu bố trí (layout_json)
 */
@Service
public class HousingService {
    private static final Logger log = LoggerFactory.getLogger(HousingService.class);

    @Autowired(required = false) private JdbcTemplate jdbc;
    @Autowired private com.fantasyrealm.inventory.InventoryManager inventory;
    @Autowired private com.fantasyrealm.social.RelationshipService relationships;

    /** Char là chủ hoặc vợ/chồng của chủ → coi như đồng sở hữu. */
    private boolean isOwnerOrSpouse(com.fantasyrealm.player.PlayerSession s, long ownerCharId) {
        if (ownerCharId == 0) return false;
        if (ownerCharId == s.getCharacterId()) return true;
        // vợ/chồng của mình có sở hữu nhà này không
        long spouse = relationships.getSpouseCharId(s.getCharacterId());
        return spouse != 0 && spouse == ownerCharId;
    }

    /** Danh sách nhà (đang bán + của mình). */
    public void onListHouses(PlayerSession s, Packet p) {
        if (jdbc == null) { s.send(notify("DB chưa sẵn sàng")); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT h.id,h.address,h.type_code,h.zone_id,h.owner_char_id,h.locked," +
            "ht.name_vn AS type_name,ht.tier,ht.purchase_price,ht.storage_slots " +
            "FROM houses h JOIN house_types ht ON h.type_code=ht.type_code " +
            "WHERE ht.is_enabled=TRUE ORDER BY h.zone_id,h.id");
        Packet pkt = new Packet(PacketType.S_HOUSE_LIST).writeInt(rows.size());
        for (Map<String,Object> r : rows) {
            long owner = lnum(r,"owner_char_id");
            pkt.writeInt(num(r,"id")).writeString(str(r,"address")).writeString(str(r,"type_name"))
               .writeString(str(r,"tier")).writeInt(num(r,"zone_id"))
               .writeLong(owner).writeBool(owner == s.getCharacterId())
               .writeLong(lnum(r,"purchase_price")).writeBool(bool(r,"locked"));
        }
        s.send(pkt);
    }

    /** Mua nhà. */
    public void onBuyHouse(PlayerSession s, Packet p) {
        int houseId = p.readInt();
        if (jdbc == null) { s.send(houseResult("DB chưa sẵn sàng", false)); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT h.owner_char_id, h.address, ht.purchase_price FROM houses h " +
            "JOIN house_types ht ON h.type_code=ht.type_code WHERE h.id=?", houseId);
        if (rows.isEmpty()) { s.send(houseResult("Nhà không tồn tại", false)); return; }
        Map<String,Object> r = rows.get(0);
        if (lnum(r,"owner_char_id") != 0) { s.send(houseResult("Nhà đã có chủ", false)); return; }
        long price = lnum(r,"purchase_price");
        if (s.getGold() < price) { s.send(houseResult("Không đủ vàng (cần " + price + ")", false)); return; }

        s.setGold(s.getGold() - price);
        jdbc.update("UPDATE houses SET owner_char_id=? WHERE id=?", s.getCharacterId(), houseId);
        s.send(houseResult("Đã mua '" + str(r,"address") + "'! Đây là nhà của bạn.", true));
        log.info("{} mua nhà #{} giá {}", s.getCharacterName(), houseId, price);
    }

    /** Vào nhà (kiểm tra quyền: chủ luôn vào được, khách cần mở khóa). */
    public void onEnterHouse(PlayerSession s, Packet p) {
        int houseId = p.readInt();
        if (jdbc == null) { s.send(notify("DB chưa sẵn sàng")); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT owner_char_id, locked, layout_json, address FROM houses WHERE id=?", houseId);
        if (rows.isEmpty()) { s.send(notify("Nhà không tồn tại")); return; }
        Map<String,Object> r = rows.get(0);
        boolean isOwner = isOwnerOrSpouse(s, lnum(r,"owner_char_id"));
        if (bool(r,"locked") && !isOwner) { s.send(notify("Cửa đang khóa")); return; }

        // Gửi nội thất trong nhà
        List<Map<String,Object>> furn = jdbc.queryForList(
            "SELECT furniture_code,pos_x,pos_y,rotation FROM house_furniture WHERE house_id=?", houseId);
        Packet pkt = new Packet(PacketType.S_HOUSE_INTERIOR)
            .writeInt(houseId).writeString(str(r,"address")).writeBool(isOwner).writeInt(furn.size());
        for (Map<String,Object> f : furn) {
            pkt.writeString(str(f,"furniture_code"))
               .writeFloat(fnum(f,"pos_x")).writeFloat(fnum(f,"pos_y")).writeInt(num(f,"rotation"));
        }
        s.send(pkt);
    }

    /** Khóa/mở cửa (chỉ chủ). */
    public void onLockHouse(PlayerSession s, Packet p) {
        int houseId = p.readInt();
        boolean lock = p.readBool();
        if (jdbc == null) return;
        Long owner = jdbc.queryForObject("SELECT owner_char_id FROM houses WHERE id=?", Long.class, houseId);
        if (owner == null || !isOwnerOrSpouse(s, owner)) { s.send(notify("Bạn không phải chủ nhà")); return; }
        jdbc.update("UPDATE houses SET locked=? WHERE id=?", lock, houseId);
        s.send(notify(lock ? "Đã khóa cửa" : "Đã mở khóa cửa"));
    }

    /** Mua nội thất (trừ vàng, thêm vào kho người chơi dạng item ảo). */
    public void onBuyFurniture(PlayerSession s, Packet p) {
        String code = p.readString();
        if (jdbc == null) { s.send(houseResult("DB chưa sẵn sàng", false)); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT name_vn,price FROM furniture_catalog WHERE furniture_code=? AND is_enabled=TRUE", code);
        if (rows.isEmpty()) { s.send(houseResult("Nội thất không tồn tại", false)); return; }
        long price = lnum(rows.get(0),"price");
        if (s.getGold() < price) { s.send(houseResult("Không đủ vàng", false)); return; }
        s.setGold(s.getGold() - price);
        s.send(houseResult("Đã mua " + str(rows.get(0),"name_vn") + " — đặt vào nhà của bạn", true));
    }

    /** Đặt nội thất trong nhà (chỉ chủ). */
    public void onPlaceFurniture(PlayerSession s, Packet p) {
        int houseId = p.readInt();
        String code = p.readString();
        float x = p.readFloat(), y = p.readFloat();
        int rot = p.readByte();
        if (jdbc == null) return;
        Long owner = jdbc.queryForObject("SELECT owner_char_id FROM houses WHERE id=?", Long.class, houseId);
        if (owner == null || !isOwnerOrSpouse(s, owner)) { s.send(notify("Bạn không phải chủ nhà")); return; }

        // Giới hạn số nội thất theo loại nhà
        Integer cur = jdbc.queryForObject("SELECT COUNT(*) FROM house_furniture WHERE house_id=?", Integer.class, houseId);
        Integer max = jdbc.queryForObject(
            "SELECT ht.max_furniture FROM houses h JOIN house_types ht ON h.type_code=ht.type_code WHERE h.id=?",
            Integer.class, houseId);
        if (cur != null && max != null && cur >= max) { s.send(notify("Nhà đã đầy nội thất")); return; }

        jdbc.update("INSERT INTO house_furniture (house_id,furniture_code,pos_x,pos_y,rotation) VALUES(?,?,?,?,?)",
            houseId, code, x, y, rot);
        // Báo mọi người trong nhà cập nhật
        s.send(new Packet(PacketType.S_FURNITURE_UPDATE)
            .writeInt(houseId).writeString(code).writeFloat(x).writeFloat(y).writeInt(rot));
    }

    /** Tương tác với nội thất: ngồi ghế, ngủ giường (hồi máu), mở rương... */
    public void onUseFurniture(PlayerSession s, Packet p) {
        String furnitureCode = p.readString();
        // Phân loại hành động theo loại nội thất
        String effect; int hpGain = 0, mpGain = 0;
        if (furnitureCode.contains("bed")) {
            // Ngủ giường: hồi đầy máu + mana
            hpGain = s.getMaxHp() - s.getHp();
            mpGain = s.getMaxMp() - s.getMp();
            s.setHp(s.getMaxHp());
            s.setMp(s.getMaxMp());
            effect = "sleep"; // client phát animation ngủ
            s.send(notify("Bạn nghỉ ngơi, hồi đầy máu và mana"));
            // Cập nhật HUD máu/mana
            s.send(new Packet(PacketType.S_PLAYER_STATS)
                .writeInt(s.getLevel()).writeInt(s.getHp()).writeInt(s.getMaxHp())
                .writeInt(s.getMp()).writeInt(s.getMaxMp())
                .writeLong(s.getExp()).writeLong(s.getLevel()*100L).writeLong(s.getGold()));
        } else if (furnitureCode.contains("chair") || furnitureCode.contains("sofa")) {
            effect = "sit"; // ngồi
        } else if (furnitureCode.contains("chest")) {
            effect = "open_chest"; // client mở UI rương
        } else {
            effect = "none";
        }
        s.send(new Packet(PacketType.S_FURNITURE_EFFECT)
            .writeString(furnitureCode).writeString(effect)
            .writeInt(hpGain).writeInt(mpGain));
    }

    private Packet houseResult(String msg, boolean ok) {
        return new Packet(PacketType.S_HOUSE_RESULT).writeBool(ok).writeString(msg);
    }
    private Packet notify(String msg) { return new Packet(PacketType.S_NOTIFY).writeString(msg); }
    private static String str(Map<String,Object> r,String k){Object v=r.get(k);return v==null?"":v.toString();}
    private static int num(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Number n?n.intValue():0;}
    private static long lnum(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Number n?n.longValue():0;}
    private static float fnum(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Number n?n.floatValue():0;}
    private static boolean bool(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Boolean b&&b;}
}
