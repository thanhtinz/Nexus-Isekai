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
 * SoundHandler — bảng âm thanh sự kiện (sound_events) admin cấu hình.
 * Client xin 1 lần (C2S_SOUND_CONFIG) → nhận map event_key→audio_key+volume,
 * sau đó phát cục bộ theo event (đánh, skill, lên cấp, quái chết, boss gầm, nút bấm...).
 * Nhạc map: gửi S2C_PLAY_BGM khi đổi map (đọc maps.bg_music).
 */
public class SoundHandler {
    private static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    /** Gửi toàn bộ sound_events đang bật cho client. */
    public static void handleConfig(GameSession s, ByteBuf in){
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> ev=SqlSafe.query(c,
                "SELECT se.event_key, se.audio_key, se.volume, a.file_path FROM sound_events se " +
                "JOIN audio_assets a ON a.asset_key=se.audio_key AND a.is_active=1 WHERE se.is_enabled=1");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_SOUND_CONFIG);
            b.writeShort(ev.size());
            for(var e:ev){
                writeStr(b,(String)e.get("event_key"));
                writeStr(b,(String)e.get("audio_key"));
                writeStr(b,(String)e.get("file_path"));
                b.writeFloat(((Number)e.get("volume")).floatValue());
            }
            s.send(b);
        }catch(Exception e){ /* không critical */ }
    }

    /** Gửi nhạc nền của map. Ưu tiên sound_events 'map_<id>_bgm' (admin override), else maps.bg_music. */
    public static void sendMapBgm(GameSession s, int mapId){
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            String bgm=null;
            Map<String,Object> ov=SqlSafe.queryOne(c,"SELECT audio_key FROM sound_events WHERE event_key=? AND is_enabled=1", "map_"+mapId+"_bgm");
            if(ov!=null) bgm=(String)ov.get("audio_key");
            if(bgm==null || bgm.isEmpty()){
                Map<String,Object> m=SqlSafe.queryOne(c,"SELECT bg_music FROM maps WHERE id=?", mapId);
                if(m!=null && m.get("bg_music")!=null) bgm=(String)m.get("bg_music");
            }
            if(bgm==null || bgm.isEmpty()) return;
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_PLAY_BGM);
            writeStr(b,bgm);
            s.send(b);
        }catch(Exception e){ /* không critical */ }
    }
}
