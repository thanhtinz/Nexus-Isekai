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
 * WelfareHandler — Hệ thống Phúc Lợi: thưởng miễn phí/nạp/ưu đãi theo phát triển nhân vật.
 * Trạng thái: 0=chưa đạt,1=có thể nhận,2=đã nhận,3=hết hạn. Engine mốc/tiến độ dùng chung.
 * fire(charId,type,amount) cập nhật tiến độ (checkin/online/level/power/topup...).
 */
public class WelfareHandler {
    static void writeStr(ByteBuf b, String s){ byte[] d=(s==null?"":s).getBytes(StandardCharsets.UTF_8); b.writeShort(d.length); b.writeBytes(d); }
    static void result(GameSession s, String m){ ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WELFARE_RESULT); writeStr(b,m); s.send(b); }

    private static int statusOf(Map<String,Object> w){
        if(((Number)w.get("is_enabled")).intValue()!=1) return 3;
        java.sql.Timestamp now=new java.sql.Timestamp(System.currentTimeMillis());
        Object en=w.get("end_at");
        if(en!=null && now.after((java.sql.Timestamp)en)) return 3; // hết hạn
        return 1;
    }

    /** Danh sách phúc lợi (cột trái) + số mốc có thể nhận. */
    public static void handleList(GameSession s, ByteBuf in){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> ws=SqlSafe.query(c,
                "SELECT id,welfare_type,name,description,icon_id,is_enabled,end_at,claim_mode,goto_feature,price_diamond, "+
                "TIMESTAMPDIFF(SECOND,NOW(),end_at) AS secs_left FROM welfare "+
                "WHERE is_enabled=1 AND (server_id=0 OR server_id=?) ORDER BY sort_order,id", p.getServerId());
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WELFARE_LIST); b.writeShort(ws.size());
            for(var w:ws){
                int wid=((Number)w.get("id")).intValue();
                syncProgress(c, p, wid, (String)w.get("welfare_type"));
                Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,claimed_json FROM welfare_progress WHERE char_id=? AND welfare_id=?", p.getCharId(), wid);
                long prog=pr==null?0:((Number)pr.get("progress")).longValue();
                String claimed=pr==null?"[]":(String)pr.get("claimed_json");
                int claimable=countClaimable(c, wid, prog, claimed);
                b.writeInt(wid);
                writeStr(b,(String)w.get("welfare_type"));
                writeStr(b,(String)w.get("name"));
                writeStr(b,(String)w.get("description"));
                b.writeInt(((Number)w.get("icon_id")).intValue());
                b.writeByte(statusOf(w));
                writeStr(b,(String)w.get("claim_mode"));
                writeStr(b, w.get("goto_feature")==null?"":(String)w.get("goto_feature"));
                b.writeInt(((Number)w.get("price_diamond")).intValue());
                b.writeInt(claimable); // số mốc có thể nhận (chấm đỏ)
                long secs=w.get("secs_left")==null?0:((Number)w.get("secs_left")).longValue();
                b.writeInt((int)Math.max(0,secs));
            }
            s.send(b);
        }catch(Exception e){ result(s,"Loi tai phuc loi"); }
    }

    /** Chi tiết (khung phải): mốc + thưởng free/premium + tiến độ. Payload [int welfareId]. */
    public static void handleDetail(GameSession s, ByteBuf in){
        int wid=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> w=SqlSafe.queryOne(c,"SELECT name,description,welfare_type,claim_mode,price_diamond FROM welfare WHERE id=?", wid);
            if(w==null){ result(s,"Phuc loi khong ton tai"); return; }
            syncProgress(c, p, wid, (String)w.get("welfare_type"));
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,claimed_json,is_premium FROM welfare_progress WHERE char_id=? AND welfare_id=?", p.getCharId(), wid);
            long prog=pr==null?0:((Number)pr.get("progress")).longValue();
            String claimed=pr==null?"[]":(String)pr.get("claimed_json");
            boolean premium=pr!=null && ((Number)pr.get("is_premium")).intValue()==1;
            List<Map<String,Object>> ms=SqlSafe.query(c,"SELECT milestone_order,requirement,reward_json,reward_premium_json,label FROM welfare_milestones WHERE welfare_id=? ORDER BY milestone_order", wid);
            ByteBuf b=Unpooled.buffer(); b.writeShort(PacketOpcode.S2C_WELFARE_DETAIL);
            b.writeInt(wid);
            writeStr(b,(String)w.get("name")); writeStr(b,(String)w.get("description"));
            writeStr(b,(String)w.get("welfare_type")); writeStr(b,(String)w.get("claim_mode"));
            b.writeLong(prog); b.writeBoolean(premium);
            b.writeInt(((Number)w.get("price_diamond")).intValue());
            b.writeShort(ms.size());
            for(var m:ms){
                int order=((Number)m.get("milestone_order")).intValue();
                b.writeInt(order);
                b.writeInt(((Number)m.get("requirement")).intValue());
                writeStr(b,(String)m.get("reward_json"));
                writeStr(b, m.get("reward_premium_json")==null?"":(String)m.get("reward_premium_json"));
                writeStr(b,(String)m.get("label"));
                boolean claimedThis=isClaimed(claimed, order);
                b.writeBoolean(claimedThis);
                b.writeBoolean(prog >= ((Number)m.get("requirement")).intValue());
            }
            s.send(b);
        }catch(Exception e){ result(s,"Loi chi tiet phuc loi"); }
    }

    /** Nhận 1 mốc. Payload [int welfareId][int milestoneOrder]. */
    public static void handleClaim(GameSession s, ByteBuf in){
        int wid=in.readInt(); int order=in.readInt(); claimOne(s, wid, order, true);
    }

    /** Nhận tất cả mốc đủ điều kiện. Payload [int welfareId]. */
    public static void handleClaimAll(GameSession s, ByteBuf in){
        int wid=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> w=SqlSafe.queryOne(c,"SELECT welfare_type FROM welfare WHERE id=?", wid);
            if(w!=null) syncProgress(c, p, wid, (String)w.get("welfare_type"));
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,claimed_json FROM welfare_progress WHERE char_id=? AND welfare_id=?", p.getCharId(), wid);
            long prog=pr==null?0:((Number)pr.get("progress")).longValue();
            String claimed=pr==null?"[]":(String)pr.get("claimed_json");
            List<Map<String,Object>> ms=SqlSafe.query(c,"SELECT milestone_order,requirement,reward_json,reward_premium_json FROM welfare_milestones WHERE welfare_id=? ORDER BY milestone_order", wid);
            boolean premium=pr!=null && SqlSafe.queryOne(c,"SELECT is_premium FROM welfare_progress WHERE char_id=? AND welfare_id=?", p.getCharId(), wid)!=null;
            int n=0;
            for(var m:ms){
                int order=((Number)m.get("milestone_order")).intValue();
                if(prog>=((Number)m.get("requirement")).intValue() && !isClaimed(claimed,order)){
                    grantReward(c,p,(String)m.get("reward_json"));
                    String prem=(String)m.get("reward_premium_json");
                    if(premium && prem!=null && !prem.isEmpty()) grantReward(c,p,prem);
                    claimed=addClaimed(claimed,order); n++;
                }
            }
            if(n==0){ result(s,"Khong co moc nao de nhan"); return; }
            SqlSafe.update(c,"INSERT INTO welfare_progress (char_id,welfare_id,progress,claimed_json) VALUES (?,?,?,?) "+
                "ON DUPLICATE KEY UPDATE claimed_json=VALUES(claimed_json)", p.getCharId(), wid, prog, claimed);
            result(s,"Da nhan "+n+" moc thuong!");
        }catch(Exception e){ result(s,"Loi nhan tat ca"); }
    }

    private static void claimOne(GameSession s, int wid, int order, boolean notify){
        Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> w=SqlSafe.queryOne(c,"SELECT welfare_type FROM welfare WHERE id=?", wid);
            if(w!=null) syncProgress(c, p, wid, (String)w.get("welfare_type"));
            Map<String,Object> m=SqlSafe.queryOne(c,"SELECT requirement,reward_json,reward_premium_json FROM welfare_milestones WHERE welfare_id=? AND milestone_order=?", wid, order);
            if(m==null){ result(s,"Moc khong ton tai"); return; }
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT progress,claimed_json,is_premium FROM welfare_progress WHERE char_id=? AND welfare_id=?", p.getCharId(), wid);
            long prog=pr==null?0:((Number)pr.get("progress")).longValue();
            String claimed=pr==null?"[]":(String)pr.get("claimed_json");
            boolean premium=pr!=null && ((Number)pr.get("is_premium")).intValue()==1;
            if(prog<((Number)m.get("requirement")).intValue()){ result(s,"Chua du dieu kien"); return; }
            if(isClaimed(claimed,order)){ result(s,"Da nhan moc nay"); return; }
            grantReward(c,p,(String)m.get("reward_json"));
            String prem=(String)m.get("reward_premium_json");
            if(premium && prem!=null && !prem.isEmpty()) grantReward(c,p,prem);
            claimed=addClaimed(claimed,order);
            SqlSafe.update(c,"INSERT INTO welfare_progress (char_id,welfare_id,progress,claimed_json) VALUES (?,?,?,?) "+
                "ON DUPLICATE KEY UPDATE claimed_json=VALUES(claimed_json)", p.getCharId(), wid, prog, claimed);
            if(notify) result(s,"Da nhan thuong!");
        }catch(Exception e){ result(s,"Loi nhan thuong"); }
    }

    /** Kích hoạt (vd nhập giftcode → dùng GiftcodeHandler; ở đây xử lý thẻ/quỹ kích hoạt sẵn mua). */
    public static void handleActivate(GameSession s, ByteBuf in){
        int wid=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> w=SqlSafe.queryOne(c,"SELECT welfare_type,goto_feature FROM welfare WHERE id=?", wid);
            if(w==null){ result(s,"Khong ton tai"); return; }
            result(s,"Mo: "+(w.get("goto_feature")==null?w.get("welfare_type"):w.get("goto_feature")));
        }catch(Exception e){ result(s,"Loi"); }
    }

    /** Mua quyền lợi (thẻ tháng/quỹ): trừ KC, đánh dấu kích hoạt + hạn. Payload [int welfareId]. */
    public static void handlePurchase(GameSession s, ByteBuf in){
        int wid=in.readInt(); Player p=s.getPlayer(); if(p==null) return;
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            Map<String,Object> w=SqlSafe.queryOne(c,"SELECT welfare_type,price_diamond,duration_days FROM welfare WHERE id=?", wid);
            if(w==null){ result(s,"Khong ton tai"); return; }
            int price=((Number)w.get("price_diamond")).intValue();
            int days=((Number)w.get("duration_days")).intValue();
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT activated_at FROM welfare_progress WHERE char_id=? AND welfare_id=?", p.getCharId(), wid);
            if(pr!=null && pr.get("activated_at")!=null){ result(s,"Da kich hoat roi"); return; }
            int rows=SqlSafe.update(c,"UPDATE characters SET diamond=diamond-? WHERE id=? AND diamond>=?", price, p.getCharId(), price);
            if(rows==0){ result(s,"Khong du kim cuong"); return; }
            String expire = days>0 ? "DATE_ADD(NOW(), INTERVAL "+days+" DAY)" : "NULL";
            SqlSafe.update(c,"INSERT INTO welfare_progress (char_id,welfare_id,progress,activated_at,expire_at,is_premium) VALUES (?,?,0,NOW(),"+expire+",1) "+
                "ON DUPLICATE KEY UPDATE activated_at=NOW(), expire_at="+expire+", is_premium=1", p.getCharId(), wid);
            ActivityHandler.fire(p.getCharId(), "spend_diamond", price); // tính tích tiêu
            result(s,"Mua quyen loi thanh cong!");
        }catch(Exception e){ result(s,"Loi mua quyen loi"); }
    }

    // ───── Engine ─────
    /** Đồng bộ tiến độ tự động theo loại. */
    private static void syncProgress(Connection c, Player p, int wid, String type) throws Exception {
        if("checkin".equals(type)){
            Map<String,Object> pr=SqlSafe.queryOne(c,"SELECT last_tick_at FROM welfare_progress WHERE char_id=? AND welfare_id=?", p.getCharId(), wid);
            boolean newDay = pr==null || pr.get("last_tick_at")==null ||
                !new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()).equals(
                 new java.text.SimpleDateFormat("yyyyMMdd").format((java.util.Date)pr.get("last_tick_at")));
            if(newDay) SqlSafe.update(c,
                "INSERT INTO welfare_progress (char_id,welfare_id,progress,last_tick_at) VALUES (?,?,1,NOW()) "+
                "ON DUPLICATE KEY UPDATE progress=progress+1, last_tick_at=NOW()", p.getCharId(), wid);
        } else if("level_gift".equals(type)){
            SqlSafe.update(c,"INSERT INTO welfare_progress (char_id,welfare_id,progress) VALUES (?,?,?) "+
                "ON DUPLICATE KEY UPDATE progress=GREATEST(progress,?)", p.getCharId(), wid, p.getLevel(), p.getLevel());
        }
        // power_gift/online/topup cập nhật qua fire() từ nơi khác
    }

    /** Hook: hệ thống khác cộng tiến độ phúc lợi (online/power/topup/achievement...). */
    public static void fire(long charId, String welfareType, long amountOrValue) {
        try(Connection c=DatabaseManager.getInstance().getConnection()){
            List<Map<String,Object>> ws=SqlSafe.query(c,"SELECT id,claim_mode FROM welfare WHERE welfare_type=? AND is_enabled=1", welfareType);
            for(var w:ws){
                int wid=((Number)w.get("id")).intValue();
                // power_gift/level_gift: lấy GIÁ TRỊ lớn nhất; còn lại cộng dồn
                if("power_gift".equals(welfareType) || "level_gift".equals(welfareType))
                    SqlSafe.update(c,"INSERT INTO welfare_progress (char_id,welfare_id,progress) VALUES (?,?,?) ON DUPLICATE KEY UPDATE progress=GREATEST(progress,?)", charId, wid, amountOrValue, amountOrValue);
                else
                    SqlSafe.update(c,"INSERT INTO welfare_progress (char_id,welfare_id,progress) VALUES (?,?,?) ON DUPLICATE KEY UPDATE progress=progress+?", charId, wid, amountOrValue, amountOrValue);
            }
        }catch(Exception e){ }
    }

    private static int countClaimable(Connection c, int wid, long prog, String claimed) throws Exception {
        List<Map<String,Object>> ms=SqlSafe.query(c,"SELECT milestone_order,requirement FROM welfare_milestones WHERE welfare_id=?", wid);
        int n=0;
        for(var m:ms){ int o=((Number)m.get("milestone_order")).intValue();
            if(prog>=((Number)m.get("requirement")).intValue() && !isClaimed(claimed,o)) n++; }
        return n;
    }
    private static boolean isClaimed(String claimed, int order){ return ("," + claimed.replaceAll("[\\[\\] ]","") + ",").contains("," + order + ","); }
    private static String addClaimed(String claimed, int order){ return claimed.equals("[]")||claimed.isEmpty() ? "["+order+"]" : claimed.replace("]", ","+order+"]"); }

    private static void grantReward(Connection c, Player p, String json) throws Exception {
        if(json==null) return;
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
}
