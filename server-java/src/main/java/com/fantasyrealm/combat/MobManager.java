package com.fantasyrealm.combat;

import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.zone.ZoneManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Quản lý quái trong thế giới: spawn theo template (admin DB), respawn sau khi chết,
 * cung cấp lookup cho CombatService.
 */
@Service
public class MobManager {
    private static final Logger log = LoggerFactory.getLogger(MobManager.class);
    private static final long RESPAWN_MS = 15_000;
    private static final int  PER_TEMPLATE = 3; // số mob mỗi template mỗi zone

    @Autowired(required = false) private JdbcTemplate jdbc;
    @Autowired private ZoneManager zoneManager;

    // mobId -> Mob
    private final Map<Long,Mob> mobs = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try { spawnFromTemplates(); }
        catch (Exception e) { log.warn("Spawn mob lỗi (DB chưa sẵn sàng?): {}", e.getMessage()); }
    }

    /** Đọc template từ bảng mobs, spawn vào các zone tương ứng. */
    public void spawnFromTemplates() {
        if (jdbc == null) { log.info("Không có DB — bỏ qua spawn mob"); return; }
        List<Map<String,Object>> rows = jdbc.queryForList(
            "SELECT template_id,name,name_vn,type,zone_ids,level_min,level_max," +
            "hp,atk,def,exp_reward FROM mobs");
        int count = 0;
        for (Map<String,Object> r : rows) {
            Integer[] zones = parseZones(r.get("zone_ids"));
            for (int zoneId : zones) {
                for (int i = 0; i < PER_TEMPLATE; i++) {
                    Mob m = buildMob(r, zoneId);
                    mobs.put(m.id, m);
                    count++;
                }
            }
        }
        log.info("Đã spawn {} mob từ {} template", count, rows.size());
    }

    private Mob buildMob(Map<String,Object> r, int zoneId) {
        int tid   = num(r.get("template_id"));
        String nm = r.get("name_vn") != null ? r.get("name_vn").toString() : String.valueOf(r.get("name"));
        String ty = r.get("type") != null ? r.get("type").toString() : "normal";
        int lvMin = num(r.get("level_min")), lvMax = num(r.get("level_max"));
        int lv    = lvMin + (lvMax > lvMin ? ThreadLocalRandom.current().nextInt(lvMax - lvMin + 1) : 0);
        int hp    = num(r.get("hp")), atk = num(r.get("atk")), def = num(r.get("def"));
        long exp  = num(r.get("exp_reward"));
        long gold = 10L + lv * 5L;
        float x = 50 + ThreadLocalRandom.current().nextInt(200);
        float y = 50 + ThreadLocalRandom.current().nextInt(200);
        return new Mob(tid, nm, ty, lv, hp, atk, def, exp, gold, zoneId, x, y);
    }

    /** Respawn quái đã chết, định kỳ. */
    @Scheduled(fixedRate = 5000)
    public void respawnTick() {
        for (Mob m : mobs.values()) {
            if (m.readyToRespawn(RESPAWN_MS)) {
                m.respawn();
                zoneManager.broadcastZone(m.zoneId, new Packet(PacketType.S_MOB_SPAWN)
                    .writeLong(m.id).writeInt(m.templateId).writeString(m.name)
                    .writeInt(m.level).writeInt(m.maxHp).writeInt(m.getHp())
                    .writeFloat(m.getX()).writeFloat(m.getY()));
            }
        }
    }

    public Mob get(long mobId) { return mobs.get(mobId); }

    public List<Mob> getMobsInZone(int zoneId) {
        List<Mob> list = new ArrayList<>();
        for (Mob m : mobs.values()) if (m.zoneId == zoneId && !m.isDead()) list.add(m);
        return list;
    }

    /** Gửi danh sách mob sống trong zone cho 1 người chơi (khi vào zone). */
    public Packet buildMobListPacket(int zoneId) {
        List<Mob> list = getMobsInZone(zoneId);
        Packet p = new Packet(PacketType.S_MOB_LIST).writeInt(list.size());
        for (Mob m : list) {
            p.writeLong(m.id).writeInt(m.templateId).writeString(m.name)
             .writeInt(m.level).writeInt(m.maxHp).writeInt(m.getHp())
             .writeFloat(m.getX()).writeFloat(m.getY());
        }
        return p;
    }

    private static int num(Object o) { return o instanceof Number ? ((Number)o).intValue() : 0; }

    @SuppressWarnings("unchecked")
    private Integer[] parseZones(Object zoneIds) {
        // PostgreSQL INT[] → java.sql.Array hoặc Integer[]
        if (zoneIds == null) return new Integer[]{1};
        try {
            if (zoneIds instanceof java.sql.Array arr) {
                Object[] a = (Object[]) arr.getArray();
                if (a.length == 0) return new Integer[]{1};
                Integer[] out = new Integer[a.length];
                for (int i = 0; i < a.length; i++) out[i] = ((Number)a[i]).intValue();
                return out;
            }
            if (zoneIds instanceof Integer[] ia) return ia.length > 0 ? ia : new Integer[]{1};
        } catch (Exception e) { /* fallthrough */ }
        return new Integer[]{1};
    }
}
