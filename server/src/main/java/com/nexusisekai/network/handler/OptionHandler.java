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
import java.util.Map;

/**
 * OptionHandler — Rút/chuyển option trang bị: lấy dòng option của đồ nguồn gắn sang
 * đồ đích. Tốn phí; theo tỉ lệ thành công; đồ nguồn có thể vỡ. Cấu hình admin.
 */
public class OptionHandler {
    private static void result(GameSession s, boolean ok, String msg){
        ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_OPTION_RESULT); b.writeByte(ok?1:0);
        byte[] d=msg.getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); s.send(b);
    }

    public static void handleExtract(GameSession s, ByteBuf in){
        long srcId=in.readLong(), dstId=in.readLong();
        Player p=s.getPlayer(); if(p==null) return;
        if(srcId==dstId){ result(s,false,"Chon 2 mon khac nhau"); return; }
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> cfg=SqlSafe.queryOne(c,"SELECT cost_gold,cost_diamond,cost_item_id,success_rate,consume_source FROM option_extract_config WHERE is_enabled=1 ORDER BY id LIMIT 1");
            if(cfg==null){ result(s,false,"Tinh nang dang tat"); return; }
            // 2 món phải của chính người chơi
            Map<String,Object> src=SqlSafe.queryOne(c,"SELECT options_json FROM character_inventory WHERE id=? AND char_id=?", srcId, p.getCharId());
            Map<String,Object> dst=SqlSafe.queryOne(c,"SELECT id FROM character_inventory WHERE id=? AND char_id=?", dstId, p.getCharId());
            if(src==null||dst==null){ result(s,false,"Trang bi khong hop le"); return; }
            String opt=(String)src.get("options_json");
            if(opt==null||opt.isBlank()){ result(s,false,"Do nguon khong co option"); return; }
            // trừ phí
            int gold=((Number)cfg.get("cost_gold")).intValue(), dia=((Number)cfg.get("cost_diamond")).intValue();
            if(gold>0 && SqlSafe.update(c,"UPDATE characters SET gold=gold-? WHERE id=? AND gold>=?", gold, p.getCharId(), gold)==0){ result(s,false,"Khong du vang"); return; }
            if(dia>0 && SqlSafe.update(c,"UPDATE characters SET diamond=diamond-? WHERE id=? AND diamond>=?", dia, p.getCharId(), dia)==0){ result(s,false,"Khong du kim cuong"); return; }
            int itemId=((Number)cfg.get("cost_item_id")).intValue();
            if(itemId>0 && SqlSafe.update(c,"UPDATE character_inventory SET qty=qty-1 WHERE char_id=? AND item_id=? AND qty>=1", p.getCharId(), itemId)==0){ result(s,false,"Thieu bua rut option"); return; }
            // tỉ lệ thành công
            int rate=((Number)cfg.get("success_rate")).intValue();
            if(Math.random()*100 >= rate){ result(s,false,"Rut that bai (mat phi)"); return; }
            // chuyển option sang đồ đích
            SqlSafe.update(c,"UPDATE character_inventory SET options_json=? WHERE id=? AND char_id=?", opt, dstId, p.getCharId());
            if(((Number)cfg.get("consume_source")).intValue()==1)
                SqlSafe.update(c,"DELETE FROM character_inventory WHERE id=? AND char_id=?", srcId, p.getCharId());
            else
                SqlSafe.update(c,"UPDATE character_inventory SET options_json=NULL WHERE id=? AND char_id=?", srcId, p.getCharId());
            result(s,true,"Rut option thanh cong");
        }catch(Exception e){ result(s,false,"Loi rut option"); }
    }
}
