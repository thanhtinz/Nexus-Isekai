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
 * AutoHandler — Auto-play. Server cấp danh sách auto cho phép theo VIP + chống bot,
 * client chạy vòng auto thật (đánh/nhặt/dùng máu/skill/nhiệm vụ/bán). Server vẫn xác
 * thực từng hành động qua handler combat/pickup bình thường (auto không bỏ qua kiểm tra).
 */
public class AutoHandler {
    // bit cờ auto theo thứ tự auto_config (attack=1,pickup=2,potion=4,skill=8,quest=16,sell=32)
    private static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    /** Gửi danh sách loại auto + VIP yêu cầu (client vẽ menu, ẩn cái chưa đủ VIP). */
    public static void sendAutoConfig(GameSession s){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            int vip=vipOf(c, p.getCharId());
            List<Map<String,Object>> cfg=SqlSafe.query(c,
                "SELECT auto_type,display_name,min_vip,max_minutes FROM auto_config WHERE is_enabled=1 ORDER BY sort_order");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_AUTO_CONFIG); b.writeShort(cfg.size());
            for(var a:cfg){
                writeStr(b,(String)a.get("auto_type")); writeStr(b,(String)a.get("display_name"));
                int minVip=((Number)a.get("min_vip")).intValue();
                b.writeInt(minVip); b.writeInt(((Number)a.get("max_minutes")).intValue());
                b.writeByte(vip>=minVip?1:0); // đủ điều kiện chưa
            }
            s.send(b);
        }catch(Exception e){ }
    }

    /** Client báo các auto muốn bật (bitmask). Server lọc theo VIP rồi trả lại cờ được phép. */
    public static void handleSetAuto(GameSession s, ByteBuf in){
        int want=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            int vip=vipOf(c, p.getCharId());
            List<Map<String,Object>> cfg=SqlSafe.query(c,
                "SELECT auto_type,min_vip FROM auto_config WHERE is_enabled=1 ORDER BY sort_order");
            int allowed=0, bit=1;
            for(var a:cfg){
                int minVip=((Number)a.get("min_vip")).intValue();
                if((want&bit)!=0 && vip>=minVip) allowed|=bit;
                bit<<=1;
            }
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_AUTO_STATE); b.writeInt(allowed); s.send(b);
        }catch(Exception e){ }
    }

    private static int vipOf(Connection c, long charId) throws Exception {
        Map<String,Object> m=SqlSafe.queryOne(c,"SELECT vip_level FROM characters WHERE id=?", charId);
        return m==null?0:((Number)m.get("vip_level")).intValue();
    }
}
