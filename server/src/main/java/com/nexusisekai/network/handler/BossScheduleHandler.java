package com.nexusisekai.network.handler;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.database.SqlSafe;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * BossScheduleHandler — Bảng giờ + định vị boss. Trả danh sách boss kèm map, toạ độ,
 * và mốc spawn kế (epoch ms) để client hiển thị đếm ngược + nút dẫn đường.
 */
public class BossScheduleHandler {
    private static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    public static void handleSchedule(GameSession s, ByteBuf in){
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> rows=SqlSafe.query(c,
                "SELECT id,boss_name,monster_id,map_id,x,y,interval_min,next_spawn_at FROM boss_schedule WHERE is_active=1 ORDER BY sort_order,id");
            long now=System.currentTimeMillis();
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_BOSS_SCHEDULE); b.writeShort(rows.size());
            for(var r:rows){
                long next=((Number)r.get("next_spawn_at")).longValue();
                int interval=((Number)r.get("interval_min")).intValue();
                // nếu mốc đã qua hoặc chưa đặt → tính mốc kế theo chu kỳ
                if(next<=now && interval>0){
                    long step=interval*60_000L;
                    long base = next<=0 ? now : next;
                    long k = (now-base)/step + 1;
                    next = base + k*step;
                    SqlSafe.update(c,"UPDATE boss_schedule SET next_spawn_at=? WHERE id=?", next, ((Number)r.get("id")).intValue());
                }
                b.writeInt(((Number)r.get("id")).intValue());
                writeStr(b,(String)r.get("boss_name"));
                b.writeInt(((Number)r.get("monster_id")).intValue());
                b.writeInt(((Number)r.get("map_id")).intValue());
                b.writeInt(((Number)r.get("x")).intValue());
                b.writeInt(((Number)r.get("y")).intValue());
                b.writeLong(next);          // mốc spawn kế (ms)
                b.writeByte(next<=now+1000?1:0); // đang sống?
            }
            s.send(b);
        }catch(Exception e){ }
    }
}
