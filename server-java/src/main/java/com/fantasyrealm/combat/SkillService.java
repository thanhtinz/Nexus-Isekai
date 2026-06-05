package com.fantasyrealm.combat;

import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Hệ thống kỹ năng: gửi danh sách skill khả dụng, dùng skill (kiểm tra mana +
 * cooldown), áp hiệu ứng (sát thương đơn/vùng, hồi máu, buff, hút máu).
 */
@Service
public class SkillService {
    private static final Logger log = LoggerFactory.getLogger(SkillService.class);

    @Autowired private MobManager  mobs;
    @Autowired private ZoneManager zoneManager;
    @Autowired private CombatService combat;

    // playerId -> (skillId -> thời điểm dùng cuối)
    private final Map<Long, Map<Integer,Long>> cooldowns = new ConcurrentHashMap<>();
    // playerId -> (buff hết hạn ms)
    private final Map<Long, Long> atkBuffUntil = new ConcurrentHashMap<>();
    private final Map<Long, Long> defBuffUntil = new ConcurrentHashMap<>();

    /** Gửi danh sách skill nhân vật dùng được. */
    public void onSkillListReq(PlayerSession s, Packet p) {
        int fac = s.getFaction() != null ? s.getFaction().id : 0;
        List<Skill> avail = new ArrayList<>();
        for (Skill sk : Skill.values())
            if (sk.availableFor(fac, s.getLevel())) avail.add(sk);

        Packet pkt = new Packet(PacketType.S_SKILL_LIST).writeInt(avail.size());
        for (Skill sk : avail) {
            pkt.writeInt(sk.id).writeString(sk.name)
               .writeInt(sk.manaCost).writeInt(sk.cooldownMs)
               .writeByte(sk.type.ordinal());
        }
        s.send(pkt);
    }

    /** Dùng skill. Payload: skillId (int), targetMobId (long, 0 nếu không nhắm). */
    public void onUseSkill(PlayerSession s, Packet p) {
        int skillId  = p.readInt();
        long targetId = p.readLong();

        if (!s.isAlive()) return;
        Skill skill = Skill.byId(skillId);
        if (skill == null) { s.send(notify("Kỹ năng không tồn tại")); return; }

        int fac = s.getFaction() != null ? s.getFaction().id : 0;
        if (!skill.availableFor(fac, s.getLevel())) {
            s.send(notify("Chưa mở khóa kỹ năng này")); return;
        }
        // Mana
        if (s.getMp() < skill.manaCost) { s.send(notify("Không đủ năng lượng")); return; }
        // Cooldown
        long now = System.currentTimeMillis();
        Map<Integer,Long> cds = cooldowns.computeIfAbsent(s.getPlayerId(), k -> new ConcurrentHashMap<>());
        Long last = cds.get(skillId);
        if (last != null && now - last < skill.cooldownMs) {
            long remain = skill.cooldownMs - (now - last);
            s.send(new Packet(PacketType.S_SKILL_COOLDOWN).writeInt(skillId).writeInt((int)remain));
            return;
        }

        // Trừ mana + ghi cooldown
        s.setMp(s.getMp() - skill.manaCost);
        cds.put(skillId, now);

        applyEffect(s, skill, targetId);

        // Gửi kết quả + cập nhật stats (mana mới)
        s.send(new Packet(PacketType.S_SKILL_RESULT)
            .writeInt(skillId).writeInt(s.getMp()).writeInt(s.getHp()));
        combat.sendStats(s);
    }

    private void applyEffect(PlayerSession s, Skill skill, long targetId) {
        int atk = effectiveAttack(s);
        switch (skill.type) {
            case DAMAGE -> {
                Mob mob = mobs.get(targetId);
                if (mob == null || mob.isDead()) { s.send(notify("Mục tiêu không hợp lệ")); return; }
                int dmg = Math.max(1, (int)(atk * skill.power) - mob.def);
                damageMob(s, mob, dmg, skill.name);
            }
            case AOE_DAMAGE -> {
                // Đánh tất cả quái trong zone gần người chơi
                int dmg = Math.max(1, (int)(atk * skill.power));
                int hitCount = 0;
                for (Mob m : mobs.getMobsInZone(s.getCurrentZoneId())) {
                    if (m.isDead()) continue;
                    if (inRange(s, m, 96f)) { damageMob(s, m, Math.max(1, dmg - m.def), skill.name); hitCount++; }
                }
                if (hitCount == 0) s.send(notify("Không trúng mục tiêu nào"));
            }
            case HEAL -> {
                int heal = (int)(s.getMaxHp() * skill.power);
                s.setHp(s.getHp() + heal);
                s.send(notify("Hồi " + heal + " máu"));
            }
            case BUFF_ATK -> {
                atkBuffUntil.put(s.getPlayerId(), System.currentTimeMillis() + 10000);
                s.send(notify("Tăng sát thương trong 10 giây"));
            }
            case BUFF_DEF -> {
                defBuffUntil.put(s.getPlayerId(), System.currentTimeMillis() + 10000);
                s.send(notify("Tăng phòng thủ trong 10 giây"));
            }
            case DRAIN -> {
                Mob mob = mobs.get(targetId);
                if (mob == null || mob.isDead()) { s.send(notify("Mục tiêu không hợp lệ")); return; }
                int dmg = Math.max(1, (int)(atk * skill.power) - mob.def);
                boolean killed = mob.takeDamage(dmg);
                int healed = dmg / 2;
                s.setHp(s.getHp() + healed);
                zoneManager.broadcastZone(mob.zoneId, new Packet(PacketType.S_MOB_DAMAGE)
                    .writeLong(mob.id).writeInt(dmg).writeInt(mob.getHp()).writeBool(true)
                    .writeLong(s.getPlayerId()));
                s.send(notify("Hút " + healed + " máu"));
                if (killed) zoneManager.broadcastZone(mob.zoneId,
                    new Packet(PacketType.S_MOB_DEATH).writeLong(mob.id).writeLong(s.getPlayerId()));
            }
        }
    }

    private void damageMob(PlayerSession s, Mob mob, int dmg, String skillName) {
        boolean killed = mob.takeDamage(dmg);
        zoneManager.broadcastZone(mob.zoneId, new Packet(PacketType.S_MOB_DAMAGE)
            .writeLong(mob.id).writeInt(dmg).writeInt(mob.getHp()).writeBool(true)
            .writeLong(s.getPlayerId()));
        if (killed) {
            zoneManager.broadcastZone(mob.zoneId, new Packet(PacketType.S_MOB_DEATH)
                .writeLong(mob.id).writeLong(s.getPlayerId()));
            // thưởng qua CombatService để dùng chung logic exp/gold/loot/levelup
            combat.rewardKill(s, mob);
        }
    }

    private int effectiveAttack(PlayerSession s) {
        int atk = s.getAttackPower();
        Long until = atkBuffUntil.get(s.getPlayerId());
        if (until != null && System.currentTimeMillis() < until) atk = (int)(atk * 1.6);
        return atk;
    }

    private boolean inRange(PlayerSession s, Mob m, float range) {
        if (s.getPosition() == null) return true;
        float dx = s.getPosition().x() - m.getX(), dy = s.getPosition().y() - m.getY();
        return dx*dx + dy*dy <= range*range;
    }

    private Packet notify(String msg) { return new Packet(PacketType.S_NOTIFY).writeString(msg); }
}
