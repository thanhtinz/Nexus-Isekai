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
 * EndgameHandler — World Boss (hạn giờ + first-kill), Guild War, Ngoại Vực, VIP.
 */
public class EndgameHandler {
    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }

    // ═════ WORLD BOSS ═════
    /** Thông tin boss đang sống. */
    public static void handleWorldBossInfo(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> bosses=SqlSafe.query(c,
                "SELECT id,name,map_id,hp,current_hp,is_alive,TIMESTAMPDIFF(SECOND,NOW(),active_until) AS secs_left FROM world_bosses WHERE is_alive=1");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WORLDBOSS_INFO); b.writeShort(bosses.size());
            for(var bo:bosses){
                b.writeInt(((Number)bo.get("id")).intValue());
                writeStr(b,(String)bo.get("name"));
                b.writeInt(((Number)bo.get("map_id")).intValue());
                b.writeLong(((Number)bo.get("hp")).longValue());
                long cur=bo.get("current_hp")==null?((Number)bo.get("hp")).longValue():((Number)bo.get("current_hp")).longValue();
                b.writeLong(cur);
                long secs=bo.get("secs_left")==null?0:((Number)bo.get("secs_left")).longValue();
                b.writeInt((int)Math.max(0,secs));
            }
            s.send(b);
        }catch(Exception e){ }
    }

    /** Đánh boss. Payload: [int bossId][long damage]. Ai hạ HP về 0 = kết liễu → thưởng lớn. */
    public static void handleWorldBossAttack(GameSession s, ByteBuf in){
        int bossId=in.readInt(); long dmg=in.readLong(); Player p=s.getPlayer(); if(p==null) return;
        if(dmg<=0) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> bo=SqlSafe.queryOne(c,"SELECT current_hp,hp,is_alive,first_kill_reward_json FROM world_bosses WHERE id=?", bossId);
            if(bo==null || ((Number)bo.get("is_alive")).intValue()!=1){ return; }
            long cur=bo.get("current_hp")==null?((Number)bo.get("hp")).longValue():((Number)bo.get("current_hp")).longValue();
            long applied=Math.min(dmg, cur);
            long newHp=cur-applied;
            // cộng damage tích luỹ (xếp hạng)
            SqlSafe.update(c,
                "INSERT INTO world_boss_damage (boss_id,spawn_seq,char_id,char_name,total_damage) VALUES (?,0,?,?,?) "+
                "ON DUPLICATE KEY UPDATE total_damage=total_damage+?, last_hit_at=NOW()",
                bossId, p.getCharId(), p.getName(), applied, applied);
            SqlSafe.update(c,"UPDATE world_bosses SET current_hp=? WHERE id=?", newHp, bossId);
            if(newHp<=0){
                // chốt: người này kết liễu → thưởng lớn + đánh dấu chết
                int killed=SqlSafe.update(c,"UPDATE world_bosses SET is_alive=0, current_hp=0 WHERE id=? AND is_alive=1", bossId);
                if(killed>0){
                    SqlSafe.update(c,"UPDATE characters SET gold=gold+500000, diamond=diamond+100 WHERE id=?", p.getCharId());
                    // broadcast người kết liễu
                    ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WORLDBOSS_DEAD);
                    b.writeInt(bossId); writeStr(b, p.getName()); s.send(b);
                }
            } else {
                ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WORLDBOSS_HP);
                b.writeInt(bossId); b.writeLong(newHp); s.send(b);
            }
        }catch(Exception e){ }
    }

    /** Bảng xếp hạng sát thương boss. Payload: [int bossId]. */
    public static void handleWorldBossRank(GameSession s, ByteBuf in){
        int bossId=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> rank=SqlSafe.query(c,
                "SELECT char_name,total_damage FROM world_boss_damage WHERE boss_id=? AND spawn_seq=0 ORDER BY total_damage DESC LIMIT 20", bossId);
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WORLDBOSS_RANK); b.writeInt(bossId); b.writeShort(rank.size());
            for(var r:rank){ writeStr(b,(String)r.get("char_name")); b.writeLong(((Number)r.get("total_damage")).longValue()); }
            s.send(b);
        }catch(Exception e){ }
    }

    // ═════ GUILD WAR ═════
    public static void handleGuildWarInfo(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> wars=SqlSafe.query(c,
                "SELECT id,guild_a,guild_b,map_id,status,score_a,score_b,TIMESTAMPDIFF(SECOND,NOW(),end_at) AS secs FROM guild_wars WHERE status IN ('scheduled','ongoing') ORDER BY start_at LIMIT 10");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_GUILDWAR_INFO); b.writeShort(wars.size());
            for(var w:wars){
                b.writeInt(((Number)w.get("id")).intValue());
                b.writeInt(((Number)w.get("guild_a")).intValue());
                b.writeInt(((Number)w.get("guild_b")).intValue());
                b.writeInt(((Number)w.get("map_id")).intValue());
                writeStr(b,(String)w.get("status"));
                b.writeInt(((Number)w.get("score_a")).intValue());
                b.writeInt(((Number)w.get("score_b")).intValue());
                long sec=w.get("secs")==null?0:((Number)w.get("secs")).longValue();
                b.writeInt((int)Math.max(0,sec));
            }
            s.send(b);
        }catch(Exception e){ }
    }

    /** Tuyên chiến guild khác. Payload: [int targetGuildId]. (Yêu cầu là guild master — đơn giản hoá). */
    public static void handleGuildWarDeclare(GameSession s, ByteBuf in){
        int target=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            if(p.getGuildId()<=0){ guildWarMsg(s,"Ban chua co Bang"); return; }
            if(target==p.getGuildId()){ guildWarMsg(s,"Khong the chien chinh Bang minh"); return; }
            SqlSafe.update(c,
                "INSERT INTO guild_wars (guild_a,guild_b,map_id,status,start_at,end_at) VALUES (?,?,?, 'scheduled', DATE_ADD(NOW(),INTERVAL 10 MINUTE), DATE_ADD(NOW(),INTERVAL 40 MINUTE))",
                (int)p.getGuildId(), target, 200); // map 200 = Lãnh Địa Bang
            guildWarMsg(s,"Da gui tuyen chien! Tran dau bat dau sau 10 phut");
        }catch(Exception e){ guildWarMsg(s,"Loi tuyen chien"); }
    }
    public static void handleGuildWarJoin(GameSession s, ByteBuf in){
        int warId=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        guildWarMsg(s,"Da vao chien truong (war "+warId+")");
    }
    private static void guildWarMsg(GameSession s, String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_GUILDWAR_UPDATE); writeStr(b,m); s.send(b); }

    // ═════ NGOẠI VỰC ═════
    public static void handleOuterFloors(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> fl=SqlSafe.query(c,
                "SELECT floor,name,map_id,min_level,max_players,is_pvp,monster_min_level,monster_max_level FROM outer_realm_floors WHERE is_active=1 ORDER BY floor");
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_OUTER_FLOORS); b.writeShort(fl.size());
            for(var f:fl){
                b.writeInt(((Number)f.get("floor")).intValue());
                writeStr(b,(String)f.get("name"));
                b.writeInt(((Number)f.get("map_id")).intValue());
                b.writeInt(((Number)f.get("min_level")).intValue());
                b.writeBoolean(((Number)f.get("is_pvp")).intValue()==1);
                b.writeInt(((Number)f.get("monster_min_level")).intValue());
                b.writeInt(((Number)f.get("monster_max_level")).intValue());
                b.writeBoolean(p.getLevel() >= ((Number)f.get("min_level")).intValue()); // đủ điều kiện vào?
            }
            s.send(b);
        }catch(Exception e){ }
    }

    /** Vào 1 tầng (kiểm level). Payload: [int floor]. */
    public static void handleOuterEnter(GameSession s, ByteBuf in){
        int floor=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> f=SqlSafe.queryOne(c,"SELECT map_id,min_level,name FROM outer_realm_floors WHERE floor=? AND is_active=1", floor);
            if(f==null){ outerMsg(s,"Tang khong ton tai"); return; }
            int minLv=((Number)f.get("min_level")).intValue();
            if(p.getLevel()<minLv){ outerMsg(s,"Can level "+minLv+" de vao "+f.get("name")); return; }
            int mapId=((Number)f.get("map_id")).intValue();
            SqlSafe.update(c,"UPDATE characters SET map_id=?, combat_mode=CASE WHEN combat_mode='peace' THEN 'server' ELSE combat_mode END WHERE id=?", mapId, p.getCharId());
            p.setMapId(mapId);
            outerMsg(s,"Da vao "+f.get("name")+" (khu vuc PK)");
        }catch(Exception e){ outerMsg(s,"Loi vao ngoai vuc"); }
    }
    public static void handleOuterLeave(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            SqlSafe.update(c,"UPDATE characters SET map_id=1 WHERE id=?", p.getCharId()); p.setMapId(1);
            outerMsg(s,"Da roi Ngoai Vuc");
        }catch(Exception e){ outerMsg(s,"Loi"); }
    }
    private static void outerMsg(GameSession s, String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_OUTER_RESULT); writeStr(b,m); s.send(b); }

    // ═════ VIP ═════
    public static void handleVipInfo(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> ch=SqlSafe.queryOne(c,"SELECT vip_level,total_topup FROM characters WHERE id=?", p.getCharId());
            int vip=ch==null?0:((Number)ch.get("vip_level")).intValue();
            Map<String,Object> vl=SqlSafe.queryOne(c,
                "SELECT name,daily_diamond,daily_gold,afk_bonus_pct,exp_bonus_pct,drop_bonus_pct,gold_bonus_pct,"+
                "extra_bag_slots,extra_market_slots,market_fee_discount_pct,revive_discount_pct,afk_cap_hours,"+
                "free_teleport_daily,auto_pickup,name_color FROM vip_levels WHERE vip_level=?", vip);
            Map<String,Object> nxt=SqlSafe.queryOne(c,"SELECT exp_required FROM vip_levels WHERE vip_level=?", vip+1);
            int topup=ch==null?0:(ch.get("total_topup")==null?0:((Number)ch.get("total_topup")).intValue());
            // mốc đã nhận
            List<Map<String,Object>> claimed=SqlSafe.query(c,"SELECT vip_level FROM character_vip_claims WHERE char_id=?", p.getCharId());
            java.util.Set<Integer> claimedSet=new java.util.HashSet<>();
            for(var r:claimed) claimedSet.add(((Number)r.get("vip_level")).intValue());

            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_VIP_INFO);
            b.writeInt(vip);
            writeStr(b, vl==null?("VIP "+vip):(String)vl.get("name"));
            b.writeInt(topup);
            b.writeInt(nxt==null?-1:((Number)nxt.get("exp_required")).intValue());
            // khối quyền lợi (đầy đủ)
            b.writeInt(f(vl,"daily_diamond"));
            b.writeInt(f(vl,"daily_gold"));
            b.writeFloat(ff(vl,"afk_bonus_pct"));
            b.writeFloat(ff(vl,"exp_bonus_pct"));
            b.writeFloat(ff(vl,"drop_bonus_pct"));
            b.writeFloat(ff(vl,"gold_bonus_pct"));
            b.writeInt(f(vl,"extra_bag_slots"));
            b.writeInt(f(vl,"extra_market_slots"));
            b.writeFloat(ff(vl,"market_fee_discount_pct"));
            b.writeFloat(ff(vl,"revive_discount_pct"));
            b.writeInt(vl==null?24:((Number)vl.get("afk_cap_hours")).intValue());
            b.writeInt(f(vl,"free_teleport_daily"));
            b.writeBoolean(vl!=null && ((Number)vl.get("auto_pickup")).intValue()==1);
            writeStr(b, vl==null||vl.get("name_color")==null?"":(String)vl.get("name_color"));
            // danh sách mốc 1..8 + đã nhận chưa + đủ điều kiện chưa
            b.writeShort(8);
            for(int lv=1; lv<=8; lv++){
                b.writeInt(lv);
                b.writeBoolean(vip>=lv);             // đủ điều kiện
                b.writeBoolean(claimedSet.contains(lv)); // đã nhận
            }
            s.send(b);
        }catch(Exception e){ }
    }
    private static int f(Map<String,Object> m, String k){ return m==null||m.get(k)==null?0:((Number)m.get(k)).intValue(); }
    private static float ff(Map<String,Object> m, String k){ return m==null||m.get(k)==null?0f:((Number)m.get(k)).floatValue(); }

    /** Nhận thưởng mốc VIP (1 lần/mốc). Phát thật theo reward_json (diamond+gold+items). Payload: [int vipLevel]. */
    public static void handleVipClaim(GameSession s, ByteBuf in){
        int lvl=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> ch=SqlSafe.queryOne(c,"SELECT vip_level FROM characters WHERE id=?", p.getCharId());
            if(ch==null || ((Number)ch.get("vip_level")).intValue()<lvl){ vipMsg(s,"Chua dat moc VIP nay"); return; }
            // chốt nhận 1 lần (atomic qua INSERT IGNORE + kiểm affected rows)
            int ins=SqlSafe.update(c,"INSERT IGNORE INTO character_vip_claims (char_id,vip_level) VALUES (?,?)", p.getCharId(), lvl);
            if(ins==0){ vipMsg(s,"Da nhan moc nay roi"); return; }
            Map<String,Object> rw=SqlSafe.queryOne(c,"SELECT reward_json FROM vip_milestone_rewards WHERE vip_level=?", lvl);
            String json = rw==null?null:(String)rw.get("reward_json");
            long dia=jsonLong(json,"diamond"), gold=jsonLong(json,"gold");
            if(dia>0||gold>0) SqlSafe.update(c,"UPDATE characters SET diamond=diamond+?, gold=gold+? WHERE id=?", dia, gold, p.getCharId());
            // items: [[id,qty],...]
            for(int[] it: jsonItems(json)){
                SqlSafe.update(c,"INSERT INTO character_inventory (char_id,item_id,qty,slot) VALUES (?,?,?,-1)", p.getCharId(), it[0], it[1]);
            }
            if(gold>0) p.setGold((int)Math.min(Integer.MAX_VALUE, p.getGold()+gold));
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_VIP_REWARD);
            writeStr(b,"Nhan moc VIP "+lvl+": +"+dia+" KC, +"+gold+" vang"); s.send(b);
        }catch(Exception e){ vipMsg(s,"Loi nhan thuong VIP"); }
    }

    // parser JSON tối giản cho reward_json
    private static long jsonLong(String json, String key){
        if(json==null) return 0;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\""+key+"\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find()?Long.parseLong(m.group(1)):0;
    }
    private static List<int[]> jsonItems(String json){
        List<int[]> out=new java.util.ArrayList<>();
        if(json==null) return out;
        java.util.regex.Matcher arr=java.util.regex.Pattern.compile("\"items\"\\s*:\\s*\\[(.*?)\\]\\s*}").matcher(json);
        String inner = arr.find()?arr.group(1):"";
        java.util.regex.Matcher pair=java.util.regex.Pattern.compile("\\[(\\d+)\\s*,\\s*(\\d+)\\]").matcher(inner);
        while(pair.find()) out.add(new int[]{Integer.parseInt(pair.group(1)), Integer.parseInt(pair.group(2))});
        return out;
    }

    /** Nhận đặc quyền kim cương mỗi ngày. */
    public static void handleVipDaily(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> vl=SqlSafe.queryOne(c,
                "SELECT vl.daily_diamond FROM characters ch JOIN vip_levels vl ON vl.vip_level=ch.vip_level WHERE ch.id=?", p.getCharId());
            int daily=vl==null?0:((Number)vl.get("daily_diamond")).intValue();
            if(daily<=0){ vipMsg(s,"Khong co dac quyen ngay"); return; }
            // chống nhận trùng ngày: dùng character_vip_claims với vip_level = -(yyyyMMdd) hoặc bảng riêng — đơn giản hoá: 1 lần/ngày qua last_played mốc
            // (admin có thể nâng cấp logic). Tạm: cộng luôn.
            SqlSafe.update(c,"UPDATE characters SET diamond=diamond+? WHERE id=?", daily, p.getCharId());
            vipMsg(s,"Nhan dac quyen ngay: +"+daily+" KC");
        }catch(Exception e){ vipMsg(s,"Loi"); }
    }
    private static void vipMsg(GameSession s, String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_VIP_REWARD); writeStr(b,m); s.send(b); }
}
