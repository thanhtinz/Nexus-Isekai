package com.fantasyrealm.rp;

import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cơ sở kinh doanh & nghề RP (quản lý động qua DB):
 *  - Thầu/mua cơ sở (trở thành chủ) → thu nhập thụ động hằng ngày
 *  - Xin việc làm thuê tại cơ sở → vào ca làm nhận lương
 *  - Nhiều ngành: admin thêm trong bảng business_types
 */
@Service
public class BusinessService {
    private static final Logger log = LoggerFactory.getLogger(BusinessService.class);

    @Autowired(required = false) private JdbcTemplate jdbc;
    @Autowired private ReputationService reputation;

    /** Gửi danh sách cơ sở (để client hiện panel công việc kiểu GTA). */
    public void onListBusiness(PlayerSession s, Packet p) {
        if (jdbc == null) { s.send(notify("DB chưa sẵn sàng")); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT b.id,b.name,b.type_code,b.zone_id,b.owner_char_id,b.is_open," +
            "bt.name_vn AS type_name,bt.category,bt.base_pay,bt.purchase_price,bt.max_employees " +
            "FROM businesses b JOIN business_types bt ON b.type_code=bt.type_code " +
            "WHERE bt.is_enabled=TRUE ORDER BY b.zone_id,b.id");
        Packet pkt = new Packet(PacketType.S_BIZ_LIST).writeInt(rows.size());
        for (Map<String,Object> r : rows) {
            pkt.writeInt(num(r,"id")).writeString(str(r,"name")).writeString(str(r,"type_name"))
               .writeString(str(r,"category")).writeInt(num(r,"zone_id"))
               .writeLong(lnum(r,"owner_char_id")).writeInt(num(r,"base_pay"))
               .writeLong(lnum(r,"purchase_price")).writeBool(bool(r,"is_open"));
        }
        s.send(pkt);
    }

    /** Thầu/mua cơ sở. */
    public void onBuyBusiness(PlayerSession s, Packet p) {
        int bizId = p.readInt();
        if (jdbc == null) { s.send(bizResult("DB chưa sẵn sàng", false)); return; }

        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT b.owner_char_id, bt.purchase_price, b.name FROM businesses b " +
            "JOIN business_types bt ON b.type_code=bt.type_code WHERE b.id=?", bizId);
        if (rows.isEmpty()) { s.send(bizResult("Cơ sở không tồn tại", false)); return; }
        Map<String,Object> r = rows.get(0);
        if (lnum(r,"owner_char_id") != 0) { s.send(bizResult("Cơ sở đã có chủ", false)); return; }

        long price = lnum(r,"purchase_price");
        if (s.getGold() < price) { s.send(bizResult("Không đủ vàng (cần " + price + ")", false)); return; }

        s.setGold(s.getGold() - price);
        jdbc.update("UPDATE businesses SET owner_char_id=? WHERE id=?", s.getCharacterId(), bizId);
        reputation.rewardKarma(s, 5); // sở hữu hợp pháp = danh tiếng tốt
        s.send(bizResult("Đã thầu '" + str(r,"name") + "' với " + price + " vàng!", true));
        log.info("{} thầu cơ sở #{} giá {}", s.getCharacterName(), bizId, price);
    }

    /** Xin làm thuê tại cơ sở. */
    public void onApply(PlayerSession s, Packet p) {
        int bizId = p.readInt();
        if (jdbc == null) { s.send(bizResult("DB chưa sẵn sàng", false)); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT bt.max_employees, " +
            "(SELECT COUNT(*) FROM business_employees e WHERE e.business_id=?) AS cur " +
            "FROM businesses b JOIN business_types bt ON b.type_code=bt.type_code WHERE b.id=?", bizId, bizId);
        if (rows.isEmpty()) { s.send(bizResult("Cơ sở không tồn tại", false)); return; }
        if (num(rows.get(0),"cur") >= num(rows.get(0),"max_employees")) {
            s.send(bizResult("Cơ sở đã đủ nhân viên", false)); return;
        }
        try {
            jdbc.update("INSERT INTO business_employees (business_id,char_id) VALUES(?,?) " +
                "ON CONFLICT DO NOTHING", bizId, s.getCharacterId());
            s.send(bizResult("Đã được nhận làm việc! Dùng /work để vào ca", true));
        } catch (Exception e) { s.send(bizResult("Bạn đã làm ở đây rồi", false)); }
    }

    /** Vào ca làm việc (nhận lương từ quỹ cơ sở / hệ thống). */
    public void onWork(PlayerSession s, Packet p) {
        int bizId = p.readInt();
        if (jdbc == null) { s.send(bizResult("DB chưa sẵn sàng", false)); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT bt.base_pay, bt.name_vn, bt.job_action FROM businesses b " +
            "JOIN business_types bt ON b.type_code=bt.type_code WHERE b.id=?", bizId);
        if (rows.isEmpty()) { s.send(bizResult("Cơ sở không tồn tại", false)); return; }

        // Kiểm tra là nhân viên hoặc chủ
        Integer isEmp = jdbc.queryForObject(
            "SELECT COUNT(*) FROM business_employees WHERE business_id=? AND char_id=?",
            Integer.class, bizId, s.getCharacterId());
        Long owner = jdbc.queryForObject("SELECT owner_char_id FROM businesses WHERE id=?", Long.class, bizId);
        boolean isOwner = owner != null && owner == s.getCharacterId();
        if ((isEmp == null || isEmp == 0) && !isOwner) {
            s.send(bizResult("Bạn không làm việc ở đây", false)); return;
        }

        int pay = num(rows.get(0),"base_pay");
        pay += ThreadLocalRandom.current().nextInt(Math.max(1, pay / 2)); // biến thiên
        s.setGold(s.getGold() + pay);
        reputation.rewardKarma(s, 1);
        jdbc.update("UPDATE business_employees SET shifts_worked=shifts_worked+1 " +
            "WHERE business_id=? AND char_id=?", bizId, s.getCharacterId());

        s.send(new Packet(PacketType.S_RP_JOB_RESULT)
            .writeString(str(rows.get(0),"job_action")).writeLong(pay).writeLong(s.getGold()));
        s.send(notify("Làm việc tại " + str(rows.get(0),"name_vn") + " nhận " + pay + " vàng"));
    }

    /** Thu nhập thụ động hằng ngày cho chủ cơ sở. */
    @Scheduled(fixedRate = 86_400_000) // mỗi 24h
    public void dailyIncome() {
        if (jdbc == null) return;
        try {
            jdbc.update("UPDATE businesses SET treasury = treasury + " +
                "(SELECT daily_income FROM business_types bt WHERE bt.type_code=businesses.type_code) " +
                "WHERE owner_char_id IS NOT NULL");
            log.info("Đã cộng thu nhập ngày cho các cơ sở có chủ");
        } catch (Exception e) { log.warn("Daily income lỗi: {}", e.getMessage()); }
    }

    private Packet bizResult(String msg, boolean ok) {
        return new Packet(PacketType.S_BIZ_RESULT).writeBool(ok).writeString(msg);
    }
    private Packet notify(String msg) { return new Packet(PacketType.S_NOTIFY).writeString(msg); }
    private static String str(Map<String,Object> r,String k){Object v=r.get(k);return v==null?"":v.toString();}
    private static int num(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Number n?n.intValue():0;}
    private static long lnum(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Number n?n.longValue():0;}
    private static boolean bool(Map<String,Object> r,String k){Object v=r.get(k);return v instanceof Boolean b&&b;}
}
