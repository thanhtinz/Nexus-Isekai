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
 * ActivityHandler — Hệ thống Hoạt Động: gom sự kiện/nhiệm vụ giới hạn thời gian.
 * Trạng thái tính theo is_enabled + start_at/end_at. Engine mốc/tiến độ dùng chung:
 * progress (tích luỹ) vs activity_milestones.requirement; claimed_json đánh dấu đã nhận.
 * Loại passive (x2_exp/x2_drop) cung cấp multiplier cho combat/exp đọc.
 */
public class ActivityHandler {
    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }
    static String readStr(ByteBuf b){ int n=b.readShort(); byte[] d=new byte[n]; b.readBytes(d); return new String(d, StandardCharsets.UTF_8); }
    static void result(GameSession s, String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_ACTIVITY_RESULT); writeStr(b,m); s.send(b); }

    /** Trạng thái: 0=chưa mở,1=đang diễn ra,2=đã kết thúc (theo enabled + thời gian). */
    private static int statusOf(Map<String,Object> a){
        if(((Number)a.get("is_enabled")).intValue()!=1) return 0;
        java.sql.Timestamp now=new java.sql.Timestamp(System.currentTimeMillis());
        Object st=a.get("start_at"), en=a.get("end_at");
        if(st!=null && now.before((java.sql.Timestamp)st)) return 0;
        if(en!=null && now.after((java.sql.Timestamp)en)) return 2;
        return 1;
    }

    /** Danh sách hoạt động (cột trái UI) + tiến độ người chơi. */
    public static void handleList(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> acts=SqlSafe.query(c,
                "SELECT id,activity_type,name,description,icon_id,is_enabled,start_at,end_at,action_type,multiplier, "+
                "TIMESTAMPDIFF(SECOND,NOW(),end_at) AS secs_left FROM activities "+
                "WHERE is_enabled=1 AND (server_id=0 OR server_id=? OR server_id=-1) ORDER BY sort_order,id", p.getServerId());
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_ACTIVITY_LIST); b.writeShort(acts.size());
            for(var a:acts){
                int aid=((Number)a.get("id")).intValue();
                Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,streak,claimed_json FROM activity_progress WHERE char_id=? AND activity_id=?", p.getCharId(), aid);
                long prog=pr==null?0:((Number)pr.get("progress")).longValue();
                String claimed=pr==null?"[]":(String)pr.get("claimed_json");
                int total=count(c,"SELECT COUNT(*) AS n FROM activity_milestones WHERE activity_id=?", aid);
                int done=claimed==null?0:claimed.split(",").length - (claimed.equals("[]")||claimed.isEmpty()?1:0);
                if(done<0) done=0;
                b.writeInt(aid);
                writeStr(b,(String)a.get("activity_type"));
                writeStr(b,(String)a.get("name"));
                writeStr(b,(String)a.get("description"));
                b.writeInt(((Number)a.get("icon_id")).intValue());
                b.writeByte(statusOf(a));
                writeStr(b,(String)a.get("action_type"));
                b.writeFloat(((Number)a.get("multiplier")).floatValue());
                b.writeLong(prog);
                b.writeInt(total);
                b.writeInt(done);
                long secs=a.get("secs_left")==null?0:((Number)a.get("secs_left")).longValue();
                b.writeInt((int)Math.max(0,secs));
            }
            s.send(b);
        }catch(Exception e){ result(s,"Loi tai hoat dong"); }
    }

    /** Chi tiết 1 hoạt động (khung phải): mốc, thưởng, tiến độ. Payload: [int activityId]. */
    public static void handleDetail(GameSession s, ByteBuf in){
        int aid=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> a=SqlSafe.queryOne(c,"SELECT name,description,activity_type,action_type,TIMESTAMPDIFF(SECOND,NOW(),start_at) AS to_start,TIMESTAMPDIFF(SECOND,NOW(),end_at) AS to_end FROM activities WHERE id=?", aid);
            if(a==null){ result(s,"Hoat dong khong ton tai"); return; }
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,streak,claimed_json FROM activity_progress WHERE char_id=? AND activity_id=?", p.getCharId(), aid);
            long prog=pr==null?0:((Number)pr.get("progress")).longValue();
            String claimed=pr==null?"[]":(String)pr.get("claimed_json");
            List<Map<String,Object>> ms=SqlSafe.query(c,"SELECT id,milestone_order,requirement,reward_json,item_cost_id,item_cost_qty,exchange_limit,label FROM activity_milestones WHERE activity_id=? ORDER BY milestone_order", aid);
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_ACTIVITY_DETAIL);
            b.writeInt(aid);
            writeStr(b,(String)a.get("name"));
            writeStr(b,(String)a.get("description"));
            writeStr(b,(String)a.get("activity_type"));
            writeStr(b,(String)a.get("action_type"));
            b.writeLong(prog);
            long toStart=a.get("to_start")==null?0:((Number)a.get("to_start")).longValue();
            long toEnd=a.get("to_end")==null?0:((Number)a.get("to_end")).longValue();
            b.writeInt((int)toStart); b.writeInt((int)toEnd);
            b.writeShort(ms.size());
            for(var m:ms){
                int order=((Number)m.get("milestone_order")).intValue();
                b.writeInt(((Number)m.get("id")).intValue());
                b.writeInt(order);
                b.writeInt(((Number)m.get("requirement")).intValue());
                writeStr(b,(String)m.get("reward_json"));
                writeStr(b,(String)m.get("label"));
                b.writeInt(((Number)m.get("item_cost_id")).intValue());
                b.writeInt(((Number)m.get("item_cost_qty")).intValue());
                b.writeInt(((Number)m.get("exchange_limit")).intValue());
                boolean claimedThis = ("," + claimed.replaceAll("[\\[\\] ]","") + ",").contains("," + order + ",");
                b.writeBoolean(claimedThis);
                b.writeBoolean(prog >= ((Number)m.get("requirement")).intValue()); // đủ điều kiện
            }
            s.send(b);
        }catch(Exception e){ result(s,"Loi chi tiet hoat dong"); }
    }

    /** Nhận thưởng mốc. Payload: [int activityId][int milestoneOrder]. */
    public static void handleClaim(GameSession s, ByteBuf in){
        int aid=in.readInt(); int order=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            // với login/spending: cập nhật progress trước khi claim
            syncProgress(c, p, aid);
            Map<String,Object> m=SqlSafe.queryOne(c,"SELECT id,requirement,reward_json FROM activity_milestones WHERE activity_id=? AND milestone_order=?", aid, order);
            if(m==null){ result(s,"Moc khong ton tai"); return; }
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,claimed_json FROM activity_progress WHERE char_id=? AND activity_id=?", p.getCharId(), aid);
            long prog=pr==null?0:((Number)pr.get("progress")).longValue();
            String claimed=pr==null?"[]":(String)pr.get("claimed_json");
            if(prog < ((Number)m.get("requirement")).intValue()){ result(s,"Chua du dieu kien"); return; }
            if(("," + claimed.replaceAll("[\\[\\] ]","") + ",").contains("," + order + ",")){ result(s,"Da nhan moc nay"); return; }
            grantReward(c, p, (String)m.get("reward_json"));
            String newClaimed = claimed.equals("[]")||claimed.isEmpty() ? "["+order+"]" : claimed.replace("]", ","+order+"]");
            SqlSafe.update(c,
                "INSERT INTO activity_progress (char_id,activity_id,progress,claimed_json) VALUES (?,?,?,?) "+
                "ON DUPLICATE KEY UPDATE claimed_json=VALUES(claimed_json)",
                p.getCharId(), aid, prog, newClaimed);
            result(s,"Da nhan thuong moc!");
        }catch(Exception e){ result(s,"Loi nhan thuong"); }
    }

    /** Đổi thưởng. Payload: [int activityId][int milestoneId]. */
    public static void handleExchange(GameSession s, ByteBuf in){
        int aid=in.readInt(); int msId=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> m=SqlSafe.queryOne(c,"SELECT reward_json,item_cost_id,item_cost_qty,exchange_limit FROM activity_milestones WHERE id=? AND activity_id=?", msId, aid);
            if(m==null){ result(s,"Vat pham doi khong ton tai"); return; }
            int costId=((Number)m.get("item_cost_id")).intValue();
            int costQty=((Number)m.get("item_cost_qty")).intValue();
            int limit=((Number)m.get("exchange_limit")).intValue();
            if(limit>0){
                int used=count(c,"SELECT COALESCE(SUM(count),0) AS n FROM activity_exchange_log WHERE char_id=? AND milestone_id=?", p.getCharId(), msId);
                if(used>=limit){ result(s,"Da het luot doi"); return; }
            }
            // trừ vật phẩm sự kiện
            if(costId>0 && costQty>0){
                int have=count(c,"SELECT COALESCE(SUM(qty),0) AS n FROM character_inventory WHERE char_id=? AND item_id=?", p.getCharId(), costId);
                if(have<costQty){ result(s,"Khong du vat pham doi"); return; }
                SqlSafe.update(c,"UPDATE character_inventory SET qty=qty-? WHERE char_id=? AND item_id=? ORDER BY qty DESC LIMIT 1", costQty, p.getCharId(), costId);
                SqlSafe.update(c,"DELETE FROM character_inventory WHERE char_id=? AND item_id=? AND qty<=0", p.getCharId(), costId);
            }
            grantReward(c, p, (String)m.get("reward_json"));
            SqlSafe.update(c,"INSERT INTO activity_exchange_log (char_id,activity_id,milestone_id,count) VALUES (?,?,?,1) ON DUPLICATE KEY UPDATE count=count+1", p.getCharId(), aid, msId);
            result(s,"Doi thuong thanh cong!");
        }catch(Exception e){ result(s,"Loi doi thuong"); }
    }

    /** Tham gia nhanh: chuyển tới map/nội dung liên quan (đơn giản hoá: trả thông báo). */
    public static void handleJoin(GameSession s, ByteBuf in){
        int aid=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> a=SqlSafe.queryOne(c,"SELECT activity_type,name FROM activities WHERE id=?", aid);
            if(a==null){ result(s,"Hoat dong khong ton tai"); return; }
            result(s,"Tham gia: "+a.get("name"));
        }catch(Exception e){ result(s,"Loi"); }
    }

    // ───── Engine dùng chung ─────
    /** Cập nhật progress tự động cho login (theo ngày) / online (theo phút) / spending (đọc total_spent). */
    private static void syncProgress(Connection c, Player p, int aid) throws Exception {
        Map<String,Object> a=SqlSafe.queryOne(c,"SELECT activity_type FROM activities WHERE id=?", aid);
        if(a==null) return;
        String type=(String)a.get("activity_type");
        if("login".equals(type)){
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,last_tick_at FROM activity_progress WHERE char_id=? AND activity_id=?", p.getCharId(), aid);
            boolean newDay = pr==null || pr.get("last_tick_at")==null ||
                !new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()).equals(
                 new java.text.SimpleDateFormat("yyyyMMdd").format((java.util.Date)pr.get("last_tick_at")));
            if(newDay){
                SqlSafe.update(c,
                    "INSERT INTO activity_progress (char_id,activity_id,progress,streak,last_tick_at) VALUES (?,?,1,1,NOW()) "+
                    "ON DUPLICATE KEY UPDATE progress=progress+1, streak=streak+1, last_tick_at=NOW()",
                    p.getCharId(), aid);
            }
        }
        // online & spending được cập nhật từ nơi khác (scheduler online tick, payment) qua addProgress()
    }

    /** API cho hệ thống khác cộng tiến độ (nhiệm vụ, tiêu KC, online...). */
    public static void addProgress(Connection c, long charId, String activityType, long amount) {
        try{
            List<Map<String,Object>> acts=SqlSafe.query(c,"SELECT id FROM activities WHERE activity_type=? AND is_enabled=1", activityType);
            for(var a:acts){
                SqlSafe.update(c,
                    "INSERT INTO activity_progress (char_id,activity_id,progress) VALUES (?,?,?) "+
                    "ON DUPLICATE KEY UPDATE progress=progress+?",
                    charId, ((Number)a.get("id")).intValue(), amount, amount);
            }
        }catch(Exception e){ /* best-effort */ }
    }

    /** Multiplier đang hiệu lực cho x2_exp / x2_drop (combat/exp đọc). 1.0 nếu không có sự kiện. */
    public static float activeMultiplier(String type) {
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> a=SqlSafe.queryOne(c,
                "SELECT multiplier FROM activities WHERE activity_type=? AND is_enabled=1 "+
                "AND (start_at IS NULL OR start_at<=NOW()) AND (end_at IS NULL OR end_at>=NOW()) "+
                "ORDER BY multiplier DESC LIMIT 1", type);
            return a==null?1.0f:((Number)a.get("multiplier")).floatValue();
        }catch(Exception e){ return 1.0f; }
    }

    private static void grantReward(Connection c, Player p, String json) throws Exception {
        long dia=jsonLong(json,"diamond"), gold=jsonLong(json,"gold");
        if(dia>0||gold>0) SqlSafe.update(c,"UPDATE characters SET diamond=diamond+?, gold=gold+? WHERE id=?", dia, gold, p.getCharId());
        if(gold>0) p.setGold((int)Math.min(Integer.MAX_VALUE, p.getGold()+gold));
        for(int[] it: jsonItems(json))
            SqlSafe.update(c,"INSERT INTO character_inventory (char_id,item_id,qty,slot) VALUES (?,?,?,-1)", p.getCharId(), it[0], it[1]);
    }
    private static long jsonLong(String json, String key){
        if(json==null) return 0;
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("\""+key+"\"\\s*:\\s*(\\d+)").matcher(json);
        return m.find()?Long.parseLong(m.group(1)):0;
    }
    private static List<int[]> jsonItems(String json){
        List<int[]> out=new java.util.ArrayList<>();
        if(json==null) return out;
        java.util.regex.Matcher arr=java.util.regex.Pattern.compile("\"items\"\\s*:\\s*\\[(.*?)\\]\\s*}").matcher(json);
        String inner=arr.find()?arr.group(1):"";
        java.util.regex.Matcher pair=java.util.regex.Pattern.compile("\\[(\\d+)\\s*,\\s*(\\d+)\\]").matcher(inner);
        while(pair.find()) out.add(new int[]{Integer.parseInt(pair.group(1)),Integer.parseInt(pair.group(2))});
        return out;
    }
    private static int count(Connection c, String sql, Object... a) throws Exception { Map<String,Object> r=SqlSafe.queryOne(c,sql,a); return r==null?0:((Number)r.get("n")).intValue(); }
}
