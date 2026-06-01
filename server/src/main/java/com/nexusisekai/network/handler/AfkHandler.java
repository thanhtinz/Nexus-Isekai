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
 * AfkHandler — Treo máy (AFK). Mua thẻ theo thời gian; nhân vật tích luỹ exp/gold/drop
 * ngay cả khi offline cho tới khi hết hạn thẻ. Nhận thưởng tích luỹ bất kỳ lúc nào.
 */
public class AfkHandler {
    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }
    static void msg(GameSession s, String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_AFK_REWARD); writeStr(b,m); s.send(b); }

    /** Danh sách thẻ AFK đang bán. */
    public static void handleCardList(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> cards=SqlSafe.query(c,"SELECT id,name,duration_hours,price_diamond,exp_rate,gold_rate,drop_rate FROM afk_cards WHERE is_active=1 ORDER BY duration_hours");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_AFK_CARD_LIST); b.writeShort(cards.size());
            for(var card:cards){
                b.writeInt(((Number)card.get("id")).intValue());
                writeStr(b,(String)card.get("name"));
                b.writeInt(((Number)card.get("duration_hours")).intValue());
                b.writeInt(((Number)card.get("price_diamond")).intValue());
                b.writeFloat(((Number)card.get("exp_rate")).floatValue());
                b.writeFloat(((Number)card.get("gold_rate")).floatValue());
                b.writeFloat(((Number)card.get("drop_rate")).floatValue());
            }
            s.send(b);
        }catch(Exception e){ msg(s,"Loi tai the AFK"); }
    }

    /** Mua thẻ (trừ diamond) + bật phiên AFK tại map hiện tại. */
    public static void handleBuy(GameSession s, ByteBuf in){
        int cardId=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> card=SqlSafe.queryOne(c,"SELECT name,duration_hours,price_diamond FROM afk_cards WHERE id=? AND is_active=1",cardId);
            if(card==null){ msg(s,"The khong ton tai"); return; }
            int price=((Number)card.get("price_diamond")).intValue();
            int hours=((Number)card.get("duration_hours")).intValue();
            // trừ diamond per-character
            int rows=SqlSafe.update(c,"UPDATE characters SET diamond=diamond-? WHERE id=? AND diamond>=?",price,p.getCharId(),price);
            if(rows==0){ msg(s,"Khong du kim cuong"); return; }
            ActivityHandler.fire(p.getCharId(), "spend_diamond", price);
            SqlSafe.update(c,
                "INSERT INTO character_afk (char_id,card_id,map_id,expires_at,last_claim_at,is_active) "+
                "VALUES (?,?,?,DATE_ADD(NOW(),INTERVAL ? HOUR),NOW(),1) "+
                "ON DUPLICATE KEY UPDATE card_id=VALUES(card_id),map_id=VALUES(map_id),expires_at=VALUES(expires_at),last_claim_at=NOW(),accrued_exp=0,accrued_gold=0,is_active=1",
                p.getCharId(),cardId,p.getMapId(),hours);
            msg(s,"Da bat AFK: "+card.get("name")+" ("+hours+"h)");
            sendStatus(s,p.getCharId());
        }catch(Exception e){ msg(s,"Loi mua the AFK"); }
    }

    /** Nhận thưởng tích luỹ (tính từ last_claim tới min(now, expires)). */
    public static void handleClaim(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> a=SqlSafe.queryOne(c,
                "SELECT ca.card_id, ca.last_claim_at, ca.expires_at, ac.exp_rate, ac.gold_rate, "+
                "LEAST(NOW(), ca.expires_at) AS until_t, TIMESTAMPDIFF(MINUTE, ca.last_claim_at, LEAST(NOW(), ca.expires_at)) AS mins "+
                "FROM character_afk ca JOIN afk_cards ac ON ac.id=ca.card_id WHERE ca.char_id=? AND ca.is_active=1", p.getCharId());
            if(a==null){ msg(s,"Chua bat AFK"); return; }
            int mins=Math.max(0,((Number)a.get("mins")).intValue());
            float expRate=((Number)a.get("exp_rate")).floatValue();
            float goldRate=((Number)a.get("gold_rate")).floatValue();
            // công thức cơ bản: mỗi phút = level*2 exp, level*5 gold, nhân hệ số + VIP bonus
            double vipBonus=1.0 + vipAfkBonus(c,p.getCharId());
            long exp=(long)(mins * (p.getLevel()*2L) * expRate * vipBonus);
            long gold=(long)(mins * (p.getLevel()*5L) * goldRate * vipBonus);
            if(mins<=0){ msg(s,"Chua co thuong moi"); return; }
            SqlSafe.update(c,"UPDATE characters SET exp=exp+?, gold=gold+? WHERE id=?", exp, gold, p.getCharId());
            SqlSafe.update(c,"UPDATE character_afk SET last_claim_at=LEAST(NOW(),expires_at), accrued_exp=accrued_exp+?, accrued_gold=accrued_gold+? WHERE char_id=?", exp, gold, p.getCharId());
            p.setGold((int)Math.min(Integer.MAX_VALUE, p.getGold()+gold));
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_AFK_REWARD);
            writeStr(b,"Nhan AFK: +"+exp+" EXP, +"+gold+" vang ("+mins+" phut)"); s.send(b);
        }catch(Exception e){ msg(s,"Loi nhan thuong AFK"); }
    }

    public static void handleStop(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            SqlSafe.update(c,"UPDATE character_afk SET is_active=0 WHERE char_id=?", p.getCharId());
            msg(s,"Da tat AFK"); sendStatus(s,p.getCharId());
        }catch(Exception e){ msg(s,"Loi"); }
    }

    public static void handleStatus(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try{ sendStatus(s,p.getCharId()); }catch(Exception e){ msg(s,"Loi"); }
    }

    private static void sendStatus(GameSession s, long charId) throws Exception {
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> a=SqlSafe.queryOne(c,
                "SELECT card_id,is_active,expires_at,accrued_exp,accrued_gold, TIMESTAMPDIFF(SECOND,NOW(),expires_at) AS secs_left FROM character_afk WHERE char_id=?", charId);
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_AFK_STATUS);
            boolean active = a!=null && ((Number)a.get("is_active")).intValue()==1 && ((Number)a.get("secs_left")).longValue()>0;
            b.writeBoolean(active);
            if(active){
                b.writeInt(((Number)a.get("card_id")).intValue());
                b.writeInt(Math.max(0,((Number)a.get("secs_left")).intValue()));
                b.writeLong(((Number)a.get("accrued_exp")).longValue());
                b.writeLong(((Number)a.get("accrued_gold")).longValue());
            }
            s.send(b);
        }
    }

    private static double vipAfkBonus(Connection c, long charId){
        try{
            Map<String,Object> v=SqlSafe.queryOne(c,
                "SELECT vl.afk_bonus_pct FROM characters ch JOIN vip_levels vl ON vl.vip_level=ch.vip_level WHERE ch.id=?", charId);
            return v==null?0.0:((Number)v.get("afk_bonus_pct")).doubleValue();
        }catch(Exception e){ return 0.0; }
    }
}
