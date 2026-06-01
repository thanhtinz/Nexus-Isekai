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
 * ClanBeastHandler — Thần Thú bang hội: thú chung của guild, thành viên góp nuôi (đổi
 * exp), lên cấp mở buff toàn bang (HP%/ATK%). Cấu hình mốc cấp/buff qua admin.
 */
public class ClanBeastHandler {
    private static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    public static void handleInfo(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        long gid=p.getGuildId(); if(gid<=0){ empty(s); return; }
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            // tạo nếu chưa có
            Map<String,Object> beast=SqlSafe.queryOne(c,"SELECT level,exp,skin_id FROM clan_beast WHERE guild_id=?", gid);
            if(beast==null){
                SqlSafe.update(c,"INSERT IGNORE INTO clan_beast (guild_id,level,exp) VALUES (?,1,0)", gid);
                beast=SqlSafe.queryOne(c,"SELECT level,exp,skin_id FROM clan_beast WHERE guild_id=?", gid);
            }
            int level=((Number)beast.get("level")).intValue();
            long exp=((Number)beast.get("exp")).longValue();
            Map<String,Object> cfg=SqlSafe.queryOne(c,"SELECT exp_need,buff_json,name FROM clan_beast_config WHERE level=?", level);
            long need=cfg==null?0:((Number)cfg.get("exp_need")).longValue();
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_CLAN_BEAST_INFO);
            b.writeByte(1); b.writeInt(level); b.writeLong(exp); b.writeLong(need);
            b.writeInt(((Number)beast.get("skin_id")).intValue());
            writeStr(b, cfg==null?"":(String)cfg.get("name"));
            writeStr(b, cfg==null?"{}":(String)cfg.get("buff_json"));
            s.send(b);
        }catch(Exception e){ empty(s); }
    }

    /** Góp nuôi: dùng vàng đổi exp cho thú (1000 vàng = 1 exp, gói theo expItems). */
    public static void handleFeed(GameSession s, ByteBuf in){
        int units=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        long gid=p.getGuildId(); if(gid<=0) return;
        if(units<=0) units=1; if(units>1000) units=1000;
        long cost=units*1000L, expAdd=units*1L;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            if(SqlSafe.update(c,"UPDATE characters SET gold=gold-? WHERE id=? AND gold>=?", cost, p.getCharId(), cost)==0) return;
            SqlSafe.update(c,"INSERT INTO clan_beast (guild_id,level,exp) VALUES (?,1,?) ON DUPLICATE KEY UPDATE exp=exp+?", gid, expAdd, expAdd);
            SqlSafe.update(c,"INSERT INTO clan_beast_feed_log (guild_id,char_id,exp_added) VALUES (?,?,?)", gid, p.getCharId(), expAdd);
            // check lên cấp
            Map<String,Object> beast=SqlSafe.queryOne(c,"SELECT level,exp FROM clan_beast WHERE guild_id=?", gid);
            int level=((Number)beast.get("level")).intValue(); long exp=((Number)beast.get("exp")).longValue();
            Map<String,Object> cfg=SqlSafe.queryOne(c,"SELECT exp_need FROM clan_beast_config WHERE level=?", level);
            while(cfg!=null){
                long need=((Number)cfg.get("exp_need")).longValue();
                if(exp< need) break;
                exp-=need; level++;
                SqlSafe.update(c,"UPDATE clan_beast SET level=?, exp=? WHERE guild_id=?", level, exp, gid);
                cfg=SqlSafe.queryOne(c,"SELECT exp_need FROM clan_beast_config WHERE level=?", level);
            }
            handleInfo(s, in);
        }catch(Exception e){ }
    }

    private static void empty(GameSession s){
        ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_CLAN_BEAST_INFO); b.writeByte(0); s.send(b);
    }
}
