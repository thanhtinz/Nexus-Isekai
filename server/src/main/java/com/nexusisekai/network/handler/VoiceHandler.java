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
 * VoiceHandler — âm thanh lời thoại cấu hình trong admin (bảng voice_lines).
 * context 1 = class_intro (giới thiệu class lúc tạo nhân vật, ref_id=class_id).
 * context 2 = npc_bark    (câu ngắn NPC khi tương tác, ref_id=npc_id).
 * Nhiều dòng cùng ref → chọn ngẫu nhiên theo weight. Trả audio_key + phụ đề.
 */
public class VoiceHandler {
    private static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    /** Payload: [byte context][int refId]. */
    public static void handleRequest(GameSession s, ByteBuf in){
        if(in.readableBytes() < 5) return;
        int ctx = in.readByte();
        int refId = in.readInt();
        String context = ctx==1 ? "class_intro" : "npc_bark";
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> lines = SqlSafe.query(c,
                "SELECT vl.audio_key, vl.subtitle, vl.weight FROM voice_lines vl " +
                "JOIN audio_assets a ON a.asset_key=vl.audio_key AND a.is_active=1 " +
                "WHERE vl.context=? AND vl.ref_id=? AND vl.is_enabled=1", context, refId);
            if(lines.isEmpty()) return; // không có thoại → im lặng
            // chọn ngẫu nhiên theo weight
            int total=0; for(var l:lines) total += Math.max(1,((Number)l.get("weight")).intValue());
            int r=(int)(Math.random()*total), acc=0;
            Map<String,Object> pick=lines.get(0);
            for(var l:lines){ acc += Math.max(1,((Number)l.get("weight")).intValue()); if(r<acc){ pick=l; break; } }
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_VOICE_PLAY);
            writeStr(b,(String)pick.get("audio_key"));
            writeStr(b,(String)pick.get("subtitle"));
            s.send(b);
        }catch(Exception e){ /* thoại không critical */ }
    }
}
