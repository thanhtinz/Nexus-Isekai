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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TreasureHandler — Kho Báu (đào rương) + Vòng Quay May Mắn.
 * Cả hai dùng chung cơ chế: trừ chi phí → roll thưởng theo trọng số → cộng thưởng.
 * Pool/segment cấu hình admin (JSON). Kho báu có giới hạn ngày; vòng quay có pity.
 */
public class TreasureHandler {
    private static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    // ═══════════ KHO BÁU ═══════════
    public static void handleTreasureList(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> cs=SqlSafe.query(c,
                "SELECT id,name,description,icon_id,cost_amount,cost_currency,cost_item_id,daily_limit FROM treasure_chests WHERE is_enabled=1 ORDER BY sort_order,id");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_TREASURE_LIST); b.writeShort(cs.size());
            for(var ch:cs){
                int id=((Number)ch.get("id")).intValue();
                int limit=((Number)ch.get("daily_limit")).intValue();
                int used=digCountToday(c, p.getCharId(), id);
                b.writeInt(id); writeStr(b,(String)ch.get("name")); writeStr(b,(String)ch.get("description"));
                b.writeInt(((Number)ch.get("icon_id")).intValue());
                b.writeInt(((Number)ch.get("cost_amount")).intValue());
                b.writeByte(((Number)ch.get("cost_currency")).intValue());
                b.writeInt(((Number)ch.get("cost_item_id")).intValue());
                b.writeInt(limit);
                b.writeInt(limit==0 ? -1 : Math.max(0, limit-used)); // còn lại (-1 = vô hạn)
            }
            s.send(b);
        }catch(Exception e){ }
    }

    public static void handleTreasureDig(GameSession s, ByteBuf in){
        int chestId=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> ch=SqlSafe.queryOne(c,"SELECT cost_amount,cost_currency,cost_item_id,daily_limit,reward_pool_json FROM treasure_chests WHERE id=? AND is_enabled=1", chestId);
            if(ch==null){ result(s, PacketOpcode.S2C_TREASURE_RESULT, "Ruong khong ton tai", ""); return; }
            int limit=((Number)ch.get("daily_limit")).intValue();
            if(limit>0 && digCountToday(c, p.getCharId(), chestId)>=limit){ result(s, PacketOpcode.S2C_TREASURE_RESULT, "Het luot dao hom nay", ""); return; }
            if(!payCost(c, p, ((Number)ch.get("cost_currency")).intValue(), ((Number)ch.get("cost_amount")).intValue(), ((Number)ch.get("cost_item_id")).intValue())){
                result(s, PacketOpcode.S2C_TREASURE_RESULT, "Khong du chi phi", ""); return; }
            SqlSafe.update(c,"INSERT INTO treasure_dig_log (char_id,chest_id,dig_date,dig_count) VALUES (?,?,CURDATE(),1) "+
                "ON DUPLICATE KEY UPDATE dig_count=dig_count+1", p.getCharId(), chestId);
            Reward r=rollPool(c, p, (String)ch.get("reward_pool_json"));
            result(s, PacketOpcode.S2C_TREASURE_RESULT, "Dao trung: "+r.label, r.label);
        }catch(Exception e){ result(s, PacketOpcode.S2C_TREASURE_RESULT, "Loi dao kho bau", ""); }
    }

    private static int digCountToday(Connection c, long charId, int chestId) throws Exception {
        Map<String,Object> m=SqlSafe.queryOne(c,"SELECT dig_count FROM treasure_dig_log WHERE char_id=? AND chest_id=? AND dig_date=CURDATE()", charId, chestId);
        return m==null?0:((Number)m.get("dig_count")).intValue();
    }

    // ═══════════ VÒNG QUAY ═══════════
    public static void handleWheelList(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> ws=SqlSafe.query(c,
                "SELECT id,name,description,icon_id,cost_amount,cost_currency,cost_item_id,segments_json,pity_count FROM lucky_wheels WHERE is_enabled=1 ORDER BY sort_order,id");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WHEEL_LIST); b.writeShort(ws.size());
            for(var w:ws){
                b.writeInt(((Number)w.get("id")).intValue());
                writeStr(b,(String)w.get("name")); writeStr(b,(String)w.get("description"));
                b.writeInt(((Number)w.get("icon_id")).intValue());
                b.writeInt(((Number)w.get("cost_amount")).intValue());
                b.writeByte(((Number)w.get("cost_currency")).intValue());
                b.writeInt(((Number)w.get("cost_item_id")).intValue());
                b.writeInt(((Number)w.get("pity_count")).intValue());
                // gửi nhãn các ô để client vẽ vòng quay
                List<Map<String,Object>> segs=parseSegs((String)w.get("segments_json"));
                b.writeShort(segs.size());
                for(var sg:segs) writeStr(b, str(sg.get("label")));
            }
            s.send(b);
        }catch(Exception e){ }
    }

    public static void handleWheelSpin(GameSession s, ByteBuf in){
        int wheelId=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> w=SqlSafe.queryOne(c,"SELECT cost_amount,cost_currency,cost_item_id,segments_json,pity_count FROM lucky_wheels WHERE id=? AND is_enabled=1", wheelId);
            if(w==null){ wheelResult(s, -1, "", "Vong quay khong ton tai"); return; }
            if(!payCost(c, p, ((Number)w.get("cost_currency")).intValue(), ((Number)w.get("cost_amount")).intValue(), ((Number)w.get("cost_item_id")).intValue())){
                wheelResult(s, -1, "", "Khong du chi phi"); return; }
            List<Map<String,Object>> segs=parseSegs((String)w.get("segments_json"));
            int pity=((Number)w.get("pity_count")).intValue();
            // cập nhật pity
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT since_pity FROM lucky_wheel_progress WHERE char_id=? AND wheel_id=?", p.getCharId(), wheelId);
            int sincePity=pr==null?0:((Number)pr.get("since_pity")).intValue();
            int idx;
            boolean pityHit = pity>0 && sincePity+1>=pity;
            if(pityHit){
                idx=-1; for(int i=0;i<segs.size();i++) if(num(segs.get(i).get("is_jackpot"))==1){ idx=i; break; }
                if(idx<0) idx=weightedPick(segs);
            } else idx=weightedPick(segs);
            Map<String,Object> seg=segs.get(idx);
            boolean jackpot=num(seg.get("is_jackpot"))==1;
            SqlSafe.update(c,"INSERT INTO lucky_wheel_progress (char_id,wheel_id,spins,since_pity) VALUES (?,?,1,?) "+
                "ON DUPLICATE KEY UPDATE spins=spins+1, since_pity=?", p.getCharId(), wheelId, jackpot?0:sincePity+1, jackpot?0:sincePity+1);
            grant(c, p, str(seg.get("type")), num(seg.get("id")), Math.max(1,num(seg.get("qty"))));
            wheelResult(s, idx, str(seg.get("label")), "Trung: "+str(seg.get("label")));
        }catch(Exception e){ wheelResult(s, -1, "", "Loi quay"); }
    }

    // ═══════════ Chung ═══════════
    static class Reward { String label; }
    private static Reward rollPool(Connection c, Player p, String json) throws Exception {
        List<Map<String,Object>> pool=parseSegs(json);
        int idx=weightedPick(pool); Map<String,Object> r=pool.get(idx);
        grant(c, p, str(r.get("type")), num(r.get("id")), Math.max(1,num(r.get("qty"))));
        Reward rw=new Reward();
        rw.label = r.get("label")!=null ? str(r.get("label")) : (str(r.get("type"))+" x"+Math.max(1,num(r.get("qty"))));
        return rw;
    }
    private static int weightedPick(List<Map<String,Object>> list){
        int total=0; for(var m:list) total+=Math.max(1,num(m.get("weight")));
        int r=(int)(Math.random()*total), acc=0;
        for(int i=0;i<list.size();i++){ acc+=Math.max(1,num(list.get(i).get("weight"))); if(r<acc) return i; }
        return list.size()-1;
    }
    private static boolean payCost(Connection c, Player p, int currency, int amount, int itemId) throws Exception {
        if(amount<=0 && currency!=2) return true;
        if(currency==0) return SqlSafe.update(c,"UPDATE characters SET gold=gold-? WHERE id=? AND gold>=?", amount, p.getCharId(), amount)>0;
        if(currency==1) return SqlSafe.update(c,"UPDATE characters SET diamond=diamond-? WHERE id=? AND diamond>=?", amount, p.getCharId(), amount)>0;
        // currency==2: trừ vật phẩm (vé/chìa)
        int qty=amount<=0?1:amount;
        return SqlSafe.update(c,"UPDATE character_inventory SET qty=qty-? WHERE char_id=? AND item_id=? AND qty>=?", qty, p.getCharId(), itemId, qty)>0;
    }
    private static void grant(Connection c, Player p, String type, int id, int qty) throws Exception {
        switch(type==null?"":type){
            case "gold"    -> SqlSafe.update(c,"UPDATE characters SET gold=gold+? WHERE id=?", qty, p.getCharId());
            case "diamond" -> SqlSafe.update(c,"UPDATE characters SET diamond=diamond+? WHERE id=?", qty, p.getCharId());
            case "item"    -> SqlSafe.update(c,"INSERT INTO character_inventory (char_id,item_id,qty,slot) VALUES (?,?,?,-1)", p.getCharId(), id, qty);
            default -> {}
        }
    }
    private static List<Map<String,Object>> parseSegs(String json){
        List<Map<String,Object>> out=new ArrayList<>();
        if(json==null) return out;
        java.util.regex.Matcher obj=java.util.regex.Pattern.compile("\\{([^}]*)\\}").matcher(json);
        while(obj.find()){
            String body=obj.group(1); Map<String,Object> m=new java.util.HashMap<>();
            java.util.regex.Matcher kv=java.util.regex.Pattern.compile("\"(\\w+)\"\\s*:\\s*(\"[^\"]*\"|\\d+)").matcher(body);
            while(kv.find()){
                String k=kv.group(1), v=kv.group(2);
                if(v.startsWith("\"")) m.put(k, v.substring(1, v.length()-1));
                else m.put(k, Integer.parseInt(v));
            }
            out.add(m);
        }
        return out;
    }
    private static int num(Object o){ return o instanceof Number n ? n.intValue() : 0; }
    private static String str(Object o){ return o==null?"":o.toString(); }
    private static void result(GameSession s, short op, String msg, String label){
        ByteBuf b=Unpooled.buffer(); b.writeShort(op); writeStr(b,msg); writeStr(b,label); s.send(b);
    }
    private static void wheelResult(GameSession s, int idx, String label, String msg){
        ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WHEEL_RESULT); b.writeInt(idx); writeStr(b,label); writeStr(b,msg); s.send(b);
    }
}
