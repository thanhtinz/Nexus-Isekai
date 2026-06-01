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
 * SoulHandler — Linh hồn quái / trứng. Giết quái có tỉ lệ rơi linh hồn (dropSoul gọi
 * từ CombatHandler khi quái chết). Người chơi gom linh hồn đổi pet/thưởng (soul_exchange).
 */
public class SoulHandler {
    private static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    /** Gọi khi quái chết: roll rơi linh hồn theo mob_soul_config. */
    public static void dropSoul(long charId, int monsterId){
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> cfg=SqlSafe.queryOne(c,"SELECT soul_id,drop_rate FROM mob_soul_config WHERE monster_id=? AND is_enabled=1", monsterId);
            if(cfg==null) return;
            int rate=((Number)cfg.get("drop_rate")).intValue();
            if(Math.random()*100 >= rate) return;
            int soulId=((Number)cfg.get("soul_id")).intValue();
            SqlSafe.update(c,"INSERT INTO char_souls (char_id,soul_id,qty) VALUES (?,?,1) ON DUPLICATE KEY UPDATE qty=qty+1", charId, soulId);
        }catch(Exception e){ }
    }

    /** Danh sách linh hồn đang có + các mục đổi. */
    public static void handleList(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> souls=SqlSafe.query(c,"SELECT soul_id,qty FROM char_souls WHERE char_id=? AND qty>0", p.getCharId());
            List<Map<String,Object>> ex=SqlSafe.query(c,"SELECT id,name,soul_id,soul_cost FROM soul_exchange WHERE is_enabled=1 ORDER BY id");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_SOUL_LIST);
            b.writeShort(souls.size());
            for(var sl:souls){ b.writeInt(((Number)sl.get("soul_id")).intValue()); b.writeInt(((Number)sl.get("qty")).intValue()); }
            b.writeShort(ex.size());
            for(var e:ex){
                b.writeInt(((Number)e.get("id")).intValue()); writeStr(b,(String)e.get("name"));
                b.writeInt(((Number)e.get("soul_id")).intValue()); b.writeInt(((Number)e.get("soul_cost")).intValue());
            }
            s.send(b);
        }catch(Exception e){ }
    }

    /** Đổi linh hồn lấy thưởng (item/pet/gold). */
    public static void handleExchange(GameSession s, ByteBuf in){
        int exId=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> e=SqlSafe.queryOne(c,"SELECT soul_id,soul_cost,reward_json FROM soul_exchange WHERE id=? AND is_enabled=1", exId);
            if(e==null){ result(s,false,"Muc doi khong ton tai"); return; }
            int soulId=((Number)e.get("soul_id")).intValue(), cost=((Number)e.get("soul_cost")).intValue();
            if(SqlSafe.update(c,"UPDATE char_souls SET qty=qty-? WHERE char_id=? AND soul_id=? AND qty>=?", cost, p.getCharId(), soulId, cost)==0){
                result(s,false,"Khong du linh hon"); return; }
            grant(c, p.getCharId(), (String)e.get("reward_json"));
            result(s,true,"Doi thanh cong");
        }catch(Exception ex){ result(s,false,"Loi doi linh hon"); }
    }

    private static void grant(Connection c, long charId, String json) throws Exception {
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\"(\\w+)\"\\s*:\\s*(\"[^\"]*\"|\\d+)").matcher(json==null?"":json);
        String type=""; int id=0, qty=1;
        while(m.find()){
            String k=m.group(1), v=m.group(2).replace("\"","");
            switch(k){ case "type"->type=v; case "id"->id=Integer.parseInt(v); case "qty"->qty=Integer.parseInt(v); }
        }
        switch(type){
            case "gold" -> SqlSafe.update(c,"UPDATE characters SET gold=gold+? WHERE id=?", qty, charId);
            case "item","pet" -> SqlSafe.update(c,"INSERT INTO character_inventory (char_id,item_id,qty,slot) VALUES (?,?,?,-1)", charId, id, Math.max(1,qty));
            default -> {}
        }
    }
    private static void result(GameSession s, boolean ok, String msg){
        ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_SOUL_RESULT); b.writeByte(ok?1:0);
        byte[] d=msg.getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); s.send(b);
    }
}
