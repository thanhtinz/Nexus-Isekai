package com.fantasyrealm.combat;

import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.zone.ZoneManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hệ thống kỹ năng nạp động từ DB (bảng skills do admin quản lý).
 * Thêm skill trong admin → reload() là server có ngay, không cần sửa code.
 */
@Service
public class SkillService {
    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    @Autowired(required = false) private JdbcTemplate jdbc;
    @Autowired private MobManager  mobs;
    @Autowired private ZoneManager zoneManager;
    @Autowired private CombatService combat;

    // code -> định nghĩa skill (nạp từ DB)
    private final Map<String, SkillDef> skillDefs = new ConcurrentHashMap<>();

    // cooldown + buff theo người chơi
    private final Map<Long, Map<String,Long>> cooldowns = new ConcurrentHashMap<>();
    private final Map<Long, Long> atkBuffUntil = new ConcurrentHashMap<>();
    private final Map<Long, Long> defBuffUntil = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try { reload(); }
        catch (Exception e) { log.warn("Nạp skill lỗi (DB chưa sẵn sàng?): {}", e.getMessage()); }
    }

    /** Nạp lại toàn bộ skill từ DB. Gọi lúc khởi động hoặc khi admin cập nhật. */
    public void reload() {
        if (jdbc == null) { log.info("Không có DB — bỏ qua nạp skill"); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT skill_code,name,name_vn,category,class_code,faction_id,effect_type,power," +
            "level_req,mana_cost,cooldown_ms,range_px,buff_duration_ms " +
            "FROM skills WHERE is_enabled=TRUE");
        skillDefs.clear();
        for (Map<String,Object> r : rows) {
            SkillDef d = new SkillDef(
                str(r,"skill_code"), str(r,"name"), str(r,"name_vn"), str(r,"category"),
                str(r,"class_code"), num(r,"faction_id"),
                str(r,"effect_type"), fnum(r,"power"),
                num(r,"level_req"), num(r,"mana_cost"), num(r,"cooldown_ms"),
                num(r,"range_px"), num(r,"buff_duration_ms"));
            skillDefs.put(d.code(), d);
        }
        log.info("Đã nạp {} skill từ DB", skillDefs.size());
    }

    /** Gửi danh sách skill nhân vật dùng được. */
    public void onSkillListReq(PlayerSession s, Packet p) {
        int fac = s.getFaction() != null ? s.getFaction().id : 0;
        List<SkillDef> avail = new ArrayList<>();
        for (SkillDef d : skillDefs.values())
            if (d.availableFor(fac, s.getLevel())) avail.add(d);

        Packet pkt = new Packet(PacketType.S_SKILL_LIST).writeInt(avail.size());
        for (SkillDef d : avail) {
            pkt.writeString(d.code()).writeString(d.nameVn() != null ? d.nameVn() : d.name())
               .writeInt(d.manaCost()).writeInt(d.cooldownMs())
               .writeString(d.effectType());
        }
        s.send(pkt);
    }

    /** Dùng skill. Payload: skillCode (string), targetMobId (long). */
    public void onUseSkill(PlayerSession s, Packet p) {
        String code   = p.readString();
        long targetId = p.readLong();

        if (!s.isAlive()) return;
        SkillDef skill = skillDefs.get(code);
        if (skill == null) { s.send(notify("Kỹ năng không tồn tại")); return; }

        int fac = s.getFaction() != null ? s.getFaction().id : 0;
        if (!skill.availableFor(fac, s.getLevel())) { s.send(notify("Chưa mở khóa kỹ năng này")); return; }
        if (s.getMp() < skill.manaCost()) { s.send(notify("Không đủ năng lượng")); return; }

        long now = System.currentTimeMillis();
        Map<String,Long> cds = cooldowns.computeIfAbsent(s.getPlayerId(), k -> new ConcurrentHashMap<>());
        Long last = cds.get(code);
        if (last != null && now - last < skill.cooldownMs()) {
            s.send(new Packet(PacketType.S_SKILL_COOLDOWN)
                .writeString(code).writeInt((int)(skill.cooldownMs() - (now - last))));
            return;
        }

        s.setMp(s.getMp() - skill.manaCost());
        cds.put(code, now);

        applyEffect(s, skill, targetId);

        s.send(new Packet(PacketType.S_SKILL_RESULT)
            .writeString(code).writeInt(s.getMp()).writeInt(s.getHp()));
        combat.sendStats(s);
    }

    private void applyEffect(PlayerSession s, SkillDef skill, long targetId) {
        int atk = effectiveAttack(s);
        switch (skill.effectType()) {
            case "damage" -> {
                Mob mob = mobs.get(targetId);
                if (mob == null || mob.isDead()) { s.send(notify("Mục tiêu không hợp lệ")); return; }
                damageMob(s, mob, Math.max(1, (int)(atk * skill.power()) - mob.def));
            }
            case "aoe_damage" -> {
                int dmg = Math.max(1, (int)(atk * skill.power()));
                int hit = 0;
                for (Mob m : mobs.getMobsInZone(s.getCurrentZoneId())) {
                    if (!m.isDead() && inRange(s, m, skill.rangePx())) {
                        damageMob(s, m, Math.max(1, dmg - m.def)); hit++;
                    }
                }
                if (hit == 0) s.send(notify("Không trúng mục tiêu nào"));
            }
            case "heal" -> {
                int heal = (int)(s.getMaxHp() * skill.power());
                s.setHp(s.getHp() + heal);
                s.send(notify("Hồi " + heal + " máu"));
            }
            case "buff_atk" -> {
                atkBuffUntil.put(s.getPlayerId(), System.currentTimeMillis() + skill.buffDurationMs());
                s.send(notify("Tăng sát thương " + skill.buffDurationMs()/1000 + " giây"));
            }
            case "buff_def" -> {
                defBuffUntil.put(s.getPlayerId(), System.currentTimeMillis() + skill.buffDurationMs());
                s.send(notify("Tăng phòng thủ " + skill.buffDurationMs()/1000 + " giây"));
            }
            case "drain" -> {
                Mob mob = mobs.get(targetId);
                if (mob == null || mob.isDead()) { s.send(notify("Mục tiêu không hợp lệ")); return; }
                int dmg = Math.max(1, (int)(atk * skill.power()) - mob.def);
                boolean killed = mob.takeDamage(dmg);
                int healed = dmg / 2;
                s.setHp(s.getHp() + healed);
                zoneManager.broadcastZone(mob.zoneId, new Packet(PacketType.S_MOB_DAMAGE)
                    .writeLong(mob.id).writeInt(dmg).writeInt(mob.getHp()).writeBool(true).writeLong(s.getPlayerId()));
                s.send(notify("Hút " + healed + " máu"));
                if (killed) combat.rewardKill(s, mob);
            }
            default -> log.debug("Hiệu ứng skill chưa hỗ trợ: {}", skill.effectType());
        }
    }

    private void damageMob(PlayerSession s, Mob mob, int dmg) {
        boolean killed = mob.takeDamage(dmg);
        zoneManager.broadcastZone(mob.zoneId, new Packet(PacketType.S_MOB_DAMAGE)
            .writeLong(mob.id).writeInt(dmg).writeInt(mob.getHp()).writeBool(true).writeLong(s.getPlayerId()));
        if (killed) combat.rewardKill(s, mob);
    }

    /** Hệ số công có tính buff. Public để CombatService dùng chung khi đánh thường. */
    public int effectiveAttack(PlayerSession s) {
        int atk = s.getAttackPower();
        Long atkUntil = atkBuffUntil.get(s.getPlayerId());
        if (atkUntil != null && System.currentTimeMillis() < atkUntil) atk = (int)(atk * 1.6);
        return atk;
    }

    /** Phòng thủ có tính buff. */
    public int effectiveDefense(PlayerSession s) {
        int def = s.getDefense();
        Long defUntil = defBuffUntil.get(s.getPlayerId());
        if (defUntil != null && System.currentTimeMillis() < defUntil) def = (int)(def * 1.6);
        return def;
    }

    private boolean inRange(PlayerSession s, Mob m, float range) {
        if (s.getPosition() == null) return true;
        float dx = s.getPosition().x() - m.getX(), dy = s.getPosition().y() - m.getY();
        return dx*dx + dy*dy <= range*range;
    }

    private Packet notify(String msg) { return new Packet(PacketType.S_NOTIFY).writeString(msg); }
    private static String str(Map<String,Object> r, String k) { Object v=r.get(k); return v==null?null:v.toString(); }
    private static int num(Map<String,Object> r, String k) { Object v=r.get(k); return v instanceof Number n?n.intValue():0; }
    private static float fnum(Map<String,Object> r, String k) { Object v=r.get(k); return v instanceof Number n?n.floatValue():1f; }
}
