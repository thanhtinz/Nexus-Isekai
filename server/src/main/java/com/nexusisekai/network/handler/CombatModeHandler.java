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
import java.util.Set;

/**
 * CombatModeHandler — chế độ chiến đấu + truy nã + nhà tù.
 * Modes: peace, guild, faction, server, berserk.
 * - peace: không PK. Map tân thủ (force_peace) ép mode này.
 * - guild/faction/server: không đánh người cùng guild/phe/server.
 * - berserk (cuồng chiến): đánh tất cả. Giết người vô tội → tăng wanted → bị mọi người đánh,
 *   nếu chết khi đang bị truy nã cao → bị nhốt tù có thời hạn.
 */
public class CombatModeHandler {
    private static final Set<String> MODES = Set.of("peace","guild","faction","server","berserk");
    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }
    static void msg(GameSession s,String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_PK_STATUS); b.writeBoolean(false); writeStr(b,m); s.send(b); }

    /** Đổi chế độ. Payload: [str mode]. */
    public static void handleSetMode(GameSession s, ByteBuf in){
        String mode=readStr(in); Player p=s.getPlayer(); if(p==null) return;
        if(!MODES.contains(mode)){ msg(s,"Che do khong hop le"); return; }
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            // map ép hoà bình → không cho đổi sang mode khác
            Map<String,Object> mp=SqlSafe.queryOne(c,"SELECT force_peace FROM maps WHERE id=?", p.getMapId());
            if(mp!=null && ((Number)mp.get("force_peace")).intValue()==1 && !mode.equals("peace")){
                msg(s,"Khu vuc nay chi cho phep che do Hoa Binh"); return;
            }
            SqlSafe.update(c,"UPDATE characters SET combat_mode=? WHERE id=?", mode, p.getCharId());
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_COMBAT_MODE); writeStr(b,mode); s.send(b);
        }catch(Exception e){ msg(s,"Loi doi che do"); }
    }

    public static void handleStatus(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> r=SqlSafe.queryOne(c,
                "SELECT combat_mode,wanted_level,pk_kills,TIMESTAMPDIFF(SECOND,NOW(),jailed_until) AS jail_secs FROM characters WHERE id=?", p.getCharId());
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_PK_STATUS); b.writeBoolean(true);
            writeStr(b,(String)r.get("combat_mode"));
            b.writeInt(((Number)r.get("wanted_level")).intValue());
            b.writeInt(((Number)r.get("pk_kills")).intValue());
            long jail = r.get("jail_secs")==null?0:((Number)r.get("jail_secs")).longValue();
            b.writeInt((int)Math.max(0,jail));
            s.send(b);
        }catch(Exception e){ msg(s,"Loi"); }
    }

    /**
     * Kiểm tra A có được phép đánh B không (gọi từ CombatHandler khi PvP).
     * Trả null nếu được phép, hoặc lý do từ chối.
     */
    public static String canAttack(Connection c, Player atk, long victimId) throws Exception {
        Map<String,Object> a=SqlSafe.queryOne(c,"SELECT combat_mode,guild_id,faction_id,server_id,TIMESTAMPDIFF(SECOND,NOW(),jailed_until) AS jail FROM characters WHERE id=?", atk.getCharId());
        Map<String,Object> v=SqlSafe.queryOne(c,"SELECT guild_id,faction_id,server_id FROM characters WHERE id=?", victimId);
        if(a==null||v==null) return "Khong tim thay muc tieu";
        long jail=a.get("jail")==null?0:((Number)a.get("jail")).longValue();
        if(jail>0) return "Ban dang bi giam, khong the tan cong";
        String mode=(String)a.get("combat_mode");
        if("peace".equals(mode)) return "Ban dang o che do Hoa Binh";
        if("guild".equals(mode) && eq(a,v,"guild_id")) return "Khong the danh nguoi cung Bang";
        if("faction".equals(mode) && eq(a,v,"faction_id")) return "Khong the danh nguoi cung Phe";
        if("server".equals(mode) && eq(a,v,"server_id")) return "Khong the danh nguoi cung Server";
        // berserk: đánh tất cả → cho phép
        return null;
    }

    /** Ghi nhận 1 lần giết PvP — cập nhật wanted/pk_kills, có thể nhốt tù nếu wanted cao. */
    public static void onPlayerKill(Connection c, long killerId, long victimId, int mapId) throws Exception {
        Map<String,Object> k=SqlSafe.queryOne(c,"SELECT combat_mode,wanted_level FROM characters WHERE id=?", killerId);
        Map<String,Object> vmode=SqlSafe.queryOne(c,"SELECT combat_mode FROM characters WHERE id=?", victimId);
        if(k==null) return;
        boolean victimInnocent = vmode!=null && "peace".equals(vmode.get("combat_mode")); // giết người không phản kháng
        SqlSafe.update(c,"INSERT INTO pk_log (killer_char_id,victim_char_id,map_id,killer_mode,victim_innocent) VALUES (?,?,?,?,?)",
            killerId, victimId, mapId, (String)k.get("combat_mode"), victimInnocent?1:0);
        SqlSafe.update(c,"UPDATE characters SET pk_kills=pk_kills+1 WHERE id=?", killerId);
        if(victimInnocent){
            int newWanted=((Number)k.get("wanted_level")).intValue()+1;
            SqlSafe.update(c,"UPDATE characters SET wanted_level=wanted_level+1 WHERE id=?", killerId);
            // wanted >= 5 → bị nhốt tù 5 phút mỗi mức vượt
            if(newWanted>=5){
                int jailMin = 5 * (newWanted-4);
                SqlSafe.update(c,"UPDATE characters SET jailed_until=DATE_ADD(NOW(),INTERVAL ? MINUTE), wanted_level=0 WHERE id=?", jailMin, killerId);
            }
        }
    }

    /** Khi người bị truy nã CHẾT → vào tù (gọi từ CombatHandler khi wanted char chết). */
    public static void onWantedDeath(Connection c, long charId) throws Exception {
        Map<String,Object> r=SqlSafe.queryOne(c,"SELECT wanted_level FROM characters WHERE id=?", charId);
        if(r==null) return;
        int wanted=((Number)r.get("wanted_level")).intValue();
        if(wanted>=3){
            int jailMin = 3 * wanted;
            SqlSafe.update(c,"UPDATE characters SET jailed_until=DATE_ADD(NOW(),INTERVAL ? MINUTE), wanted_level=0 WHERE id=?", jailMin, charId);
        }
    }

    private static boolean eq(Map<String,Object> a, Map<String,Object> b, String col){
        Object x=a.get(col), y=b.get(col);
        if(x==null||y==null) return false;
        long xv=((Number)x).longValue(), yv=((Number)y).longValue();
        return xv!=0 && xv==yv;
    }
    private static String readStr(ByteBuf b){ int n=b.readShort(); byte[] d=new byte[n]; b.readBytes(d); return new String(d, StandardCharsets.UTF_8); }
}
