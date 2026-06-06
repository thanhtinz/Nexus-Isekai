package com.fantasyrealm.housing;

import com.fantasyrealm.inventory.InventoryManager;
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
 * Phòng trọ cho thuê (người mới) + rương kho mở rộng túi đồ.
 *  - Phòng trọ: thuê theo ngày, nội thất CỐ ĐỊNH, có rương mặc định
 *  - Rương: kho phụ (trong nhà mua hoặc phòng thuê), gửi/lấy đồ, mua thêm rương
 */
@Service
public class RentalStorageService {
    private static final Logger log = LoggerFactory.getLogger(RentalStorageService.class);

    @Autowired(required = false) private JdbcTemplate jdbc;
    @Autowired private InventoryManager inventory;

    // ── Phòng trọ ──────────────────────────────────────────────
    public void onListRentals(PlayerSession s, Packet p) {
        if (jdbc == null) { s.send(notify("DB chưa sẵn sàng")); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT room_code,name_vn,name,zone_id,rent_per_day,default_storage_slots " +
            "FROM rental_rooms WHERE is_enabled=TRUE ORDER BY rent_per_day");
        Packet pkt = new Packet(PacketType.S_RENTAL_LIST).writeInt(rows.size());
        for (Map<String,Object> r : rows) {
            pkt.writeString(str(r,"room_code"))
               .writeString(r.get("name_vn") != null ? str(r,"name_vn") : str(r,"name"))
               .writeInt(num(r,"zone_id")).writeLong(lnum(r,"rent_per_day"))
               .writeInt(num(r,"default_storage_slots"));
        }
        s.send(pkt);
    }

    /** Thuê phòng (trừ tiền, tạo rương mặc định nếu chưa có). */
    public void onRentRoom(PlayerSession s, Packet p) {
        String roomCode = p.readString();
        int days = Math.max(1, p.readInt());
        if (jdbc == null) { s.send(rentalResult("DB chưa sẵn sàng", false)); return; }

        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT rent_per_day, default_storage_slots, name_vn FROM rental_rooms WHERE room_code=?", roomCode);
        if (rows.isEmpty()) { s.send(rentalResult("Phòng không tồn tại", false)); return; }
        long cost = lnum(rows.get(0),"rent_per_day") * days;
        if (s.getGold() < cost) { s.send(rentalResult("Không đủ vàng (cần " + cost + ")", false)); return; }

        s.setGold(s.getGold() - cost);
        jdbc.update("INSERT INTO room_rentals (room_code,char_id,rented_until) " +
            "VALUES(?,?, NOW() + (? || ' days')::interval) " +
            "ON CONFLICT (char_id) DO UPDATE SET room_code=?, rented_until=NOW() + (? || ' days')::interval",
            roomCode, s.getCharacterId(), days, roomCode, days);

        // Tạo rương mặc định cho phòng trọ nếu chưa có
        Integer existing = jdbc.queryForObject(
            "SELECT COUNT(*) FROM storage_chests WHERE owner_char_id=? AND location_type='rental'",
            Integer.class, s.getCharacterId());
        if (existing == null || existing == 0) {
            int slots = num(rows.get(0),"default_storage_slots");
            jdbc.update("INSERT INTO storage_chests (owner_char_id,location_type,location_id,slots,label) " +
                "VALUES(?,'rental',0,?,'Rương phòng trọ')", s.getCharacterId(), slots);
        }
        s.send(rentalResult("Đã thuê " + str(rows.get(0),"name_vn") + " " + days + " ngày", true));
        log.info("{} thuê phòng {} ({} ngày)", s.getCharacterName(), roomCode, days);
    }

    // ── Rương kho ──────────────────────────────────────────────
    public void onListChests(PlayerSession s, Packet p) {
        if (jdbc == null) { s.send(notify("DB chưa sẵn sàng")); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT id,location_type,location_id,slots,label," +
            "(SELECT COUNT(*) FROM chest_items ci WHERE ci.chest_id=storage_chests.id) AS used " +
            "FROM storage_chests WHERE owner_char_id=? ORDER BY id", s.getCharacterId());
        Packet pkt = new Packet(PacketType.S_CHEST_LIST).writeInt(rows.size());
        for (Map<String,Object> r : rows) {
            pkt.writeInt(num(r,"id")).writeString(str(r,"label"))
               .writeString(str(r,"location_type")).writeInt(num(r,"slots")).writeInt(num(r,"used"));
        }
        s.send(pkt);
    }

    /** Mua rương mới đặt trong nhà/phòng (mở rộng kho). */
    public void onBuyChest(PlayerSession s, Packet p) {
        String locationType = p.readString(); // house | rental
        int locationId = p.readInt();
        int slots = 20;
        long price = 5000L;
        if (jdbc == null) { s.send(rentalResult("DB chưa sẵn sàng", false)); return; }
        if (s.getGold() < price) { s.send(rentalResult("Không đủ vàng (cần " + price + ")", false)); return; }
        s.setGold(s.getGold() - price);
        jdbc.update("INSERT INTO storage_chests (owner_char_id,location_type,location_id,slots,label) " +
            "VALUES(?,?,?,?,'Rương')", s.getCharacterId(), locationType, locationId, slots);
        s.send(rentalResult("Đã mua rương (" + slots + " ô)", true));
    }

    /** Xem đồ trong rương. */
    public void onChestContent(PlayerSession s, Packet p) {
        sendChestContent(s, p.readInt());
    }

    private void sendChestContent(PlayerSession s, int chestId) {
        if (jdbc == null) return;
        if (!ownsChest(s, chestId)) { s.send(notify("Không phải rương của bạn")); return; }
        List<Map<String,Object>> items = jdbc.queryForList(
            "SELECT item_id,quantity FROM chest_items WHERE chest_id=?", chestId);
        Packet pkt = new Packet(PacketType.S_CHEST_CONTENT).writeInt(chestId).writeInt(items.size());
        for (Map<String,Object> it : items)
            pkt.writeLong(lnum(it,"item_id")).writeInt(num(it,"quantity"));
        s.send(pkt);
    }

    /** Cất đồ từ túi vào rương. */
    public void onStore(PlayerSession s, Packet p) {
        int chestId = p.readInt();
        long itemId = p.readLong();
        int qty = p.readInt();
        if (jdbc == null) return;
        if (!ownsChest(s, chestId)) { s.send(notify("Không phải rương của bạn")); return; }
        if (!inventory.has(s.getPlayerId(), itemId, qty)) { s.send(notify("Không đủ đồ trong túi")); return; }

        // Kiểm tra rương còn ô
        Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM chest_items WHERE chest_id=?", Integer.class, chestId);
        Integer slots = jdbc.queryForObject("SELECT slots FROM storage_chests WHERE id=?", Integer.class, chestId);
        Integer hasItem = jdbc.queryForObject("SELECT COUNT(*) FROM chest_items WHERE chest_id=? AND item_id=?",
            Integer.class, chestId, itemId);
        if (hasItem == 0 && used != null && slots != null && used >= slots) {
            s.send(notify("Rương đã đầy")); return;
        }

        inventory.remove(s.getPlayerId(), itemId, qty);
        jdbc.update("INSERT INTO chest_items (chest_id,item_id,quantity) VALUES(?,?,?) " +
            "ON CONFLICT (chest_id,item_id) DO UPDATE SET quantity = chest_items.quantity + ?",
            chestId, itemId, qty, qty);
        inventory.sendInventory(s);
        sendChestContent(s, chestId);
        s.send(notify("Đã cất " + qty + " vật phẩm vào rương"));
    }

    /** Lấy đồ từ rương về túi. */
    public void onTake(PlayerSession s, Packet p) {
        int chestId = p.readInt();
        long itemId = p.readLong();
        int qty = p.readInt();
        if (jdbc == null) return;
        if (!ownsChest(s, chestId)) { s.send(notify("Không phải rương của bạn")); return; }
        if (inventory.isFull(s.getPlayerId())) { s.send(notify("Túi đã đầy")); return; }

        Integer have = jdbc.queryForObject("SELECT quantity FROM chest_items WHERE chest_id=? AND item_id=?",
            Integer.class, chestId, itemId);
        if (have == null || have < qty) { s.send(notify("Rương không đủ đồ")); return; }

        if (have == qty) jdbc.update("DELETE FROM chest_items WHERE chest_id=? AND item_id=?", chestId, itemId);
        else jdbc.update("UPDATE chest_items SET quantity=quantity-? WHERE chest_id=? AND item_id=?", qty, chestId, itemId);
        inventory.add(s.getPlayerId(), itemId, qty);
        inventory.sendInventory(s);
        sendChestContent(s, chestId);
        s.send(notify("Đã lấy " + qty + " vật phẩm"));
    }

    private boolean ownsChest(PlayerSession s, int chestId) {
        Long owner = jdbc.queryForObject("SELECT owner_char_id FROM storage_chests WHERE id=?", Long.class, chestId);
        return owner != null && owner == s.getCharacterId();
    }

    private Packet rentalResult(String msg, boolean ok) {
        return new Packet(PacketType.S_RENTAL_RESULT).writeBool(ok).writeString(msg);
    }
    private Packet notify(String msg) { return new Packet(PacketType.S_NOTIFY).writeString(msg); }
    private static String str(Map<String,Object> r,String k){Object v=r.get(k);return v==null?"":v.toString();}
    private static int num(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Number n?n.intValue():0;}
    private static long lnum(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Number n?n.longValue():0;}
}
