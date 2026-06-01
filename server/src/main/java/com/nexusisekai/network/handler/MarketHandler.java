package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * MarketHandler — Chợ người chơi. User đăng bán vật phẩm giá cố định (vàng HOẶC kim cương),
 * user khác mua. Item bị khoá khỏi túi khi đăng; trả lại khi huỷ/hết hạn.
 */
public class MarketHandler {
    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }
    static void result(GameSession s, String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_MARKET_RESULT); writeStr(b,m); s.send(b); }

    /** Xem chợ: filter category + currency (0=all). Payload: [str category][int currency][int page]. */
    public static void handleList(GameSession s, ByteBuf in){
        String cat=readStr(in); int currency=in.readInt(); int page=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            StringBuilder w=new StringBuilder("WHERE status='active'");
            if(cat!=null && !cat.isEmpty() && !cat.equals("all")) w.append(" AND category='").append(cat.replace("'","")).append("'");
            if(currency==1||currency==2) w.append(" AND currency=").append(currency);
            int off=Math.max(0,page)*20;
            List<Map<String,Object>> rows=SqlSafe.query(c,
                "SELECT id,seller_name,item_id,item_name,qty,enhance_level,currency,price,category FROM market_listings "+w+
                " ORDER BY listed_at DESC LIMIT 20 OFFSET "+off);
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_MARKET_LIST); b.writeShort(rows.size());
            for(var r:rows){
                b.writeLong(((Number)r.get("id")).longValue());
                writeStr(b,(String)r.get("seller_name"));
                b.writeInt(((Number)r.get("item_id")).intValue());
                writeStr(b,(String)r.get("item_name"));
                b.writeInt(((Number)r.get("qty")).intValue());
                b.writeInt(((Number)r.get("enhance_level")).intValue());
                b.writeByte(((Number)r.get("currency")).intValue());
                b.writeLong(((Number)r.get("price")).longValue());
                writeStr(b,(String)r.get("category"));
            }
            s.send(b);
        }catch(Exception e){ result(s,"Loi tai cho"); }
    }

    /** Đăng bán. Payload: [long inventoryId][int qty][byte currency][long price]. */
    public static void handleSell(GameSession s, ByteBuf in){
        long invId=in.readLong(); int qty=in.readInt(); int currency=in.readByte(); long price=in.readLong();
        Player p=s.getPlayer(); if(p==null) return;
        if(price<=0 || qty<=0 || (currency!=1&&currency!=2)){ result(s,"Thong tin khong hop le"); return; }
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> it=SqlSafe.queryOne(c,
                "SELECT ci.qty AS have, ci.item_id, ci.enhance_level, COALESCE(i.name,'') AS name, COALESCE(i.category,'misc') AS category, ci.is_equipped "+
                "FROM character_inventory ci LEFT JOIN items i ON i.id=ci.item_id WHERE ci.id=? AND ci.char_id=?", invId, p.getCharId());
            if(it==null){ result(s,"Khong tim thay vat pham"); return; }
            if(((Number)it.get("is_equipped")).intValue()==1){ result(s,"Hay thao trang bi truoc"); return; }
            int have=((Number)it.get("have")).intValue();
            if(have<qty){ result(s,"Khong du so luong"); return; }
            // kiểm sức chứa chợ (số listing đang active + VIP bonus)
            int active=count(c,"SELECT COUNT(*) AS n FROM market_listings WHERE seller_char_id=? AND status='active'", p.getCharId());
            int maxSlots=5 + vipMarketSlots(c,p.getCharId());
            if(active>=maxSlots){ result(s,"Da dat gioi han gian hang ("+maxSlots+")"); return; }
            // khoá item khỏi túi
            SqlSafe.update(c,"UPDATE character_inventory SET qty=qty-? WHERE id=?", qty, invId);
            SqlSafe.update(c,"DELETE FROM character_inventory WHERE id=? AND qty<=0", invId);
            SqlSafe.update(c,
                "INSERT INTO market_listings (seller_char_id,seller_name,inventory_id,item_id,item_name,qty,enhance_level,currency,price,category,expires_at) "+
                "VALUES (?,?,?,?,?,?,?,?,?,?,DATE_ADD(NOW(),INTERVAL 3 DAY))",
                p.getCharId(), p.getName(), invId, ((Number)it.get("item_id")).intValue(), (String)it.get("name"),
                qty, ((Number)it.get("enhance_level")).intValue(), currency, price, (String)it.get("category"));
            ActivityHandler.fire(p.getCharId(), "market_sell", 1);
            result(s,"Da dang ban "+qty+"x "+it.get("name")+" gia "+price+(currency==1?" vang":" KC"));
        }catch(Exception e){ result(s,"Loi dang ban"); }
    }

    /** Mua. Payload: [long listingId]. */
    public static void handleBuy(GameSession s, ByteBuf in){
        long lid=in.readLong(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> l=SqlSafe.queryOne(c,"SELECT * FROM market_listings WHERE id=? AND status='active'", lid);
            if(l==null){ result(s,"Mat hang khong con"); return; }
            long seller=((Number)l.get("seller_char_id")).longValue();
            if(seller==p.getCharId()){ result(s,"Khong the mua hang cua chinh minh"); return; }
            int currency=((Number)l.get("currency")).intValue();
            long price=((Number)l.get("price")).longValue();
            int itemId=((Number)l.get("item_id")).intValue();
            int qty=((Number)l.get("qty")).intValue();
            int enh=((Number)l.get("enhance_level")).intValue();
            String col = currency==1?"gold":"diamond";
            // trừ tiền người mua (atomic)
            int paid=SqlSafe.update(c,"UPDATE characters SET "+col+"="+col+"-? WHERE id=? AND "+col+">=?", price, p.getCharId(), price);
            if(paid==0){ result(s, currency==1?"Khong du vang":"Khong du kim cuong"); return; }
            // đánh dấu đã bán (atomic, tránh mua trùng)
            int sold=SqlSafe.update(c,"UPDATE market_listings SET status='sold', buyer_char_id=?, sold_at=NOW() WHERE id=? AND status='active'", p.getCharId(), lid);
            if(sold==0){ // ai đó mua trước → hoàn tiền
                SqlSafe.update(c,"UPDATE characters SET "+col+"="+col+"+? WHERE id=?", price, p.getCharId());
                result(s,"Mat hang vua duoc mua boi nguoi khac"); return;
            }
            // giao item cho người mua
            SqlSafe.update(c,
                "INSERT INTO character_inventory (char_id,item_id,qty,slot,enhance_level) VALUES (?,?,?,-1,?)",
                p.getCharId(), itemId, qty, enh);
            // trả tiền cho người bán
            SqlSafe.update(c,"UPDATE characters SET "+col+"="+col+"+? WHERE id=?", price, seller);
            // ghi nhat ky tien te (phat hien RMT/chuyen vang qua cho)
            String curName = currency==1?"gold":"diamond";
            com.nexusisekai.game.economy.CurrencyLog.log(p.getCharId(), curName, -price, 0, "market_buy", "listing "+lid+" item "+itemId);
            com.nexusisekai.game.economy.CurrencyLog.log(seller, curName, price, 0, "market_sell", "listing "+lid+" item "+itemId);
            if(currency==1) p.setGold((int)Math.max(0, p.getGold()-price));
            ActivityHandler.fire(p.getCharId(), "market_buy", 1);
            result(s,"Da mua "+qty+"x "+l.get("item_name")+" het "+price+(currency==1?" vang":" KC"));
        }catch(Exception e){ result(s,"Loi mua hang"); }
    }

    /** Huỷ bán → trả item về túi. Payload: [long listingId]. */
    public static void handleCancel(GameSession s, ByteBuf in){
        long lid=in.readLong(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> l=SqlSafe.queryOne(c,"SELECT item_id,qty,enhance_level FROM market_listings WHERE id=? AND seller_char_id=? AND status='active'", lid, p.getCharId());
            if(l==null){ result(s,"Khong tim thay gian hang"); return; }
            int upd=SqlSafe.update(c,"UPDATE market_listings SET status='cancelled' WHERE id=? AND status='active'", lid);
            if(upd==0){ result(s,"That bai"); return; }
            SqlSafe.update(c,"INSERT INTO character_inventory (char_id,item_id,qty,slot,enhance_level) VALUES (?,?,?,-1,?)",
                p.getCharId(), ((Number)l.get("item_id")).intValue(), ((Number)l.get("qty")).intValue(), ((Number)l.get("enhance_level")).intValue());
            result(s,"Da huy ban, tra item ve tui");
        }catch(Exception e){ result(s,"Loi huy ban"); }
    }

    /** Hàng của tôi. */
    public static void handleMine(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> rows=SqlSafe.query(c,
                "SELECT id,item_id,item_name,qty,currency,price,status FROM market_listings WHERE seller_char_id=? AND status IN ('active','sold') ORDER BY listed_at DESC LIMIT 30", p.getCharId());
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_MARKET_LIST); b.writeShort(rows.size());
            for(var r:rows){
                b.writeLong(((Number)r.get("id")).longValue());
                writeStr(b,(String)r.get("status"));
                b.writeInt(((Number)r.get("item_id")).intValue());
                writeStr(b,(String)r.get("item_name"));
                b.writeInt(((Number)r.get("qty")).intValue());
                b.writeInt(0);
                b.writeByte(((Number)r.get("currency")).intValue());
                b.writeLong(((Number)r.get("price")).longValue());
                writeStr(b,"mine");
            }
            s.send(b);
        }catch(Exception e){ result(s,"Loi"); }
    }

    private static String readStr(ByteBuf b){ int n=b.readShort(); byte[] d=new byte[n]; b.readBytes(d); return new String(d, StandardCharsets.UTF_8); }
    private static int count(Connection c, String sql, Object... a) throws Exception { Map<String,Object> r=SqlSafe.queryOne(c,sql,a); return r==null?0:((Number)r.get("n")).intValue(); }
    private static int vipMarketSlots(Connection c, long charId){
        try{ Map<String,Object> v=SqlSafe.queryOne(c,"SELECT vl.extra_market_slots FROM characters ch JOIN vip_levels vl ON vl.vip_level=ch.vip_level WHERE ch.id=?", charId);
            return v==null?0:((Number)v.get("extra_market_slots")).intValue(); }catch(Exception e){ return 0; }
    }
}
