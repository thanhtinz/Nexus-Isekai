package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import com.nexusisekai.game.entity.Player;
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

/** ChildHandler — shop con cái (thời trang/ăn/uống/tả/bảo mẫu) + tương tác NPC trong nhà. */
public class ChildHandler {
    private static final Logger log = LoggerFactory.getLogger(ChildHandler.class);

    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }
    static String readStr(ByteBuf b){ int n=b.readShort(); byte[] d=new byte[n]; b.readBytes(d); return new String(d,StandardCharsets.UTF_8); }
    static void msg(GameSession s,String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_SERVER_MSG); writeStr(b,m); s.send(b); }

    /** Danh sách shop con cái. */
    public static void handleChildShop(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            List<Map<String,Object>> items = SqlSafe.query(c,
                "SELECT id,name,category,gold_price,diamond_price,fashion_slot,fashion_id,nanny_hours,icon_id FROM child_shop_items WHERE is_active=1");
            ByteBuf p=Unpooled.buffer(); p.writeShort(PacketOpcode.S2C_CHILD_SHOP); p.writeShort(items.size());
            for (var it : items) {
                p.writeInt(((Number)it.get("id")).intValue());
                writeStr(p,(String)it.get("name")); writeStr(p,(String)it.get("category"));
                p.writeInt(((Number)it.get("gold_price")).intValue());
                p.writeInt(((Number)it.get("diamond_price")).intValue());
                writeStr(p,(String)it.get("fashion_slot"));
                p.writeInt(((Number)it.get("fashion_id")).intValue());
                p.writeInt(((Number)it.get("nanny_hours")).intValue());
                p.writeInt(((Number)it.get("icon_id")).intValue());
            }
            s.send(p);
        } catch (Exception e){ log.warn("child shop",e); msg(s,"Loi shop con"); }
    }

    /** Mua món cho 1 đứa con → áp dụng hiệu ứng. */
    public static void handleChildBuy(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        long childId = buf.readLong(); int itemId = buf.readInt();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            var item = SqlSafe.queryOne(c, "SELECT * FROM child_shop_items WHERE id=? AND is_active=1", itemId);
            if (item == null) { msg(s,"Mon khong ton tai"); return; }
            // xác thực con thuộc về marriage của người chơi
            var child = SqlSafe.queryOne(c,
                "SELECT ch.id FROM children ch JOIN marriages m ON m.id=ch.marriage_id " +
                "WHERE ch.id=? AND (m.char_id_a=? OR m.char_id_b=?)", childId, p.getCharId(), p.getCharId());
            if (child == null) { msg(s,"Day khong phai con cua ban"); return; }

            int gold=((Number)item.get("gold_price")).intValue();
            int dia=((Number)item.get("diamond_price")).intValue();
            // TODO: trừ gold/diamond của char (kiểm tra đủ)
            if (gold>0) SqlSafe.update(c,"UPDATE characters SET gold=gold-? WHERE id=? AND gold>=?", gold, p.getCharId(), gold);
            if (dia>0)  SqlSafe.update(c,"UPDATE characters SET diamond=diamond-? WHERE id=? AND diamond>=?", dia, p.getCharId(), dia);

            String cat=(String)item.get("category");
            switch (cat) {
                case "food"   -> SqlSafe.update(c,"UPDATE children SET hunger=LEAST(100,hunger+?), happiness=LEAST(100,happiness+5) WHERE id=?", extract(item,"effect_json","hunger"), childId);
                case "drink"  -> SqlSafe.update(c,"UPDATE children SET thirst=LEAST(100,thirst+?), happiness=LEAST(100,happiness+5) WHERE id=?", extract(item,"effect_json","thirst"), childId);
                case "diaper" -> SqlSafe.update(c,"UPDATE children SET cleanliness=100, happiness=LEAST(100,happiness+10) WHERE id=?", childId);
                case "fashion"-> {
                    String slot=(String)item.get("fashion_slot"); int fid=((Number)item.get("fashion_id")).intValue();
                    if ("head".equals(slot)) SqlSafe.update(c,"UPDATE children SET fashion_head=? WHERE id=?", fid, childId);
                    else SqlSafe.update(c,"UPDATE children SET fashion_body=? WHERE id=?", fid, childId);
                }
                case "nanny"  -> {
                    int hrs=((Number)item.get("nanny_hours")).intValue();
                    SqlSafe.update(c,"UPDATE children SET nanny_until=DATE_ADD(NOW(), INTERVAL ? HOUR) WHERE id=?", hrs, childId);
                }
            }
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_CHILD_BUY);
            pkt.writeLong(childId); pkt.writeInt(itemId); pkt.writeBoolean(true); s.send(pkt);
        } catch (Exception e){ log.warn("child buy",e); msg(s,"Loi mua do cho con"); }
    }

    /** Thuê bảo mẫu (alias mua nanny qua handleChildBuy nhưng có cổng riêng). */
    public static void handleChildHireNanny(GameSession s, ByteBuf buf) {
        handleChildBuy(s, buf);
    }

    /** Tương tác với con trong nhà (chơi/ôm) → tăng happiness. */
    public static void handleChildInteract(GameSession s, ByteBuf buf) {
        if (!s.isInGame()) return;
        Player p = s.getPlayer();
        long childId = buf.readLong();
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            int updated = SqlSafe.update(c,
                "UPDATE children ch JOIN marriages m ON m.id=ch.marriage_id " +
                "SET ch.happiness=LEAST(100,ch.happiness+15) " +
                "WHERE ch.id=? AND (m.char_id_a=? OR m.char_id_b=?)", childId, p.getCharId(), p.getCharId());
            if (updated == 0) { msg(s,"Khong phai con cua ban"); return; }
            ByteBuf pkt=Unpooled.buffer(); pkt.writeShort(PacketOpcode.S2C_CHILD_INTERACT);
            pkt.writeLong(p.getCharId()); pkt.writeLong(childId); s.send(pkt);
        } catch (Exception e){ msg(s,"Loi tuong tac"); }
    }

    private static int extract(Map<String,Object> item, String col, String key) {
        String json=(String)item.get(col); if(json==null) return 0;
        int i=json.indexOf("\""+key+"\""); if(i<0) return 0;
        int colon=json.indexOf(':',i); if(colon<0) return 0;
        StringBuilder sb=new StringBuilder(); int j=colon+1;
        while(j<json.length() && Character.isDigit(json.charAt(j))){ sb.append(json.charAt(j)); j++; }
        return sb.length()>0?Integer.parseInt(sb.toString()):0;
    }
}
