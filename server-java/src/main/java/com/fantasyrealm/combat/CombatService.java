package com.fantasyrealm.combat;

import com.fantasyrealm.inventory.InventoryManager;
import com.fantasyrealm.player.PlayerSession;
import com.fantasyrealm.player.SessionManager;
import com.fantasyrealm.protocol.Packet;
import com.fantasyrealm.protocol.PacketType;
import com.fantasyrealm.zone.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Xử lý chiến đấu PvE: người chơi đánh quái, sát thương, chết, thưởng exp/gold/loot,
 * lên cấp, hồi sinh. Quái phản đòn người tấn công.
 */
@Service
public class CombatService {
    private static final Logger log = LoggerFactory.getLogger(CombatService.class);
    private static final long ATTACK_COOLDOWN_MS = 600;   // chống spam đánh
    private static final float ATTACK_RANGE = 48f;        // tầm đánh
    private static final long RESPAWN_DELAY_MS = 5000;

    @Autowired private MobManager      mobs;
    @Autowired private SessionManager  sessions;
    @Autowired private ZoneManager     zoneManager;
    @Autowired private InventoryManager inventory;

    /** Người chơi tấn công quái. */
    public void onAttackMob(PlayerSession s, Packet p) {
        long mobId = p.readLong();

        if (!s.isAlive()) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Bạn đã gục, cần hồi sinh"));
            return;
        }
        // Cooldown chống spam
        long now = System.currentTimeMillis();
        if (now - s.getLastAttackMs() < ATTACK_COOLDOWN_MS) return;
        s.setLastAttackMs(now);

        Mob mob = mobs.get(mobId);
        if (mob == null || mob.isDead()) {
            s.send(new Packet(PacketType.S_NOTIFY).writeString("Mục tiêu không tồn tại"));
            return;
        }
        // Kiểm tra tầm đánh
        if (s.getPosition() != null) {
            float dx = s.getPosition().x() - mob.getX();
            float dy = s.getPosition().y() - mob.getY();
            if (dx*dx + dy*dy > ATTACK_RANGE * ATTACK_RANGE) {
                s.send(new Packet(PacketType.S_NOTIFY).writeString("Mục tiêu quá xa"));
                return;
            }
        }

        // Tính sát thương người → quái (có biến thiên + chí mạng)
        int base = Math.max(1, s.getAttackPower() - mob.def);
        boolean crit = ThreadLocalRandom.current().nextInt(100) < 15;
        int dmg = crit ? (int)(base * 1.8) : base;
        dmg += ThreadLocalRandom.current().nextInt(Math.max(1, base / 4));

        boolean killed = mob.takeDamage(dmg);

        // Broadcast sát thương cho cả zone
        zoneManager.broadcastZone(mob.zoneId, new Packet(PacketType.S_MOB_DAMAGE)
            .writeLong(mob.id).writeInt(dmg).writeInt(mob.getHp()).writeBool(crit)
            .writeLong(s.getPlayerId()));

        if (killed) {
            onMobKilled(s, mob);
        } else {
            // Quái phản đòn người tấn công
            mob.setTargetPlayerId(s.getPlayerId());
            mobCounterAttack(s, mob);
        }
    }

    /** Quái đánh trả người chơi. */
    private void mobCounterAttack(PlayerSession s, Mob mob) {
        int dmg = Math.max(1, mob.atk - s.getDefense());
        dmg += ThreadLocalRandom.current().nextInt(Math.max(1, mob.atk / 4));
        s.setHp(s.getHp() - dmg);

        s.send(new Packet(PacketType.S_PLAYER_DAMAGE)
            .writeLong(s.getPlayerId()).writeInt(dmg).writeInt(s.getHp()).writeLong(mob.id));

        if (!s.isAlive()) onPlayerDeath(s, mob);
    }

    private void onMobKilled(PlayerSession s, Mob mob) {
        // Thưởng exp + gold
        s.setExp(s.getExp() + mob.expReward);
        s.setGold(s.getGold() + mob.goldReward);

        // Loot: 30% rơi vật phẩm thường, boss/elite cao hơn
        int lootChance = switch (mob.type) { case "boss" -> 90; case "elite" -> 50; default -> 30; };
        long lootItem = 0;
        if (ThreadLocalRandom.current().nextInt(100) < lootChance) {
            lootItem = 2001L; // item mẫu (bình máu) — mở rộng theo loot table sau
            inventory.add(s.getCharacterId(), lootItem, 1);
        }

        zoneManager.broadcastZone(mob.zoneId, new Packet(PacketType.S_MOB_DEATH)
            .writeLong(mob.id).writeLong(s.getPlayerId()));

        s.send(new Packet(PacketType.S_NOTIFY)
            .writeString("Hạ " + mob.name + " +" + mob.expReward + " EXP +" + mob.goldReward + " vàng"));

        // Kiểm tra lên cấp
        checkLevelUp(s);
        sendStats(s);
        log.debug("{} hạ {} (exp={}, gold={}, loot={})",
            s.getCharacterName(), mob.name, mob.expReward, mob.goldReward, lootItem);
    }

    private void onPlayerDeath(PlayerSession s, Mob killer) {
        s.send(new Packet(PacketType.S_PLAYER_DEATH)
            .writeLong(s.getPlayerId()).writeString("Bạn bị " + killer.name + " hạ gục"));
        zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_PLAYER_DEATH)
            .writeLong(s.getPlayerId()).writeString(""));
        // Phạt nhẹ: mất 5% gold
        long penalty = s.getGold() / 20;
        s.setGold(s.getGold() - penalty);
        log.debug("{} bị {} hạ", s.getCharacterName(), killer.name);
    }

    /** Người chơi hồi sinh. */
    public void onPlayerRespawn(PlayerSession s, Packet p) {
        if (s.isAlive()) return;
        s.setMaxHp(s.hpForLevel());
        s.setHp(s.getMaxHp());
        s.send(new Packet(PacketType.S_PLAYER_RESPAWN)
            .writeLong(s.getPlayerId()).writeInt(s.getHp())
            .writeFloat(100).writeFloat(100)); // điểm hồi sinh (làng)
        sendStats(s);
    }

    /** Công thức exp lên cấp: level * 100. */
    private void checkLevelUp(PlayerSession s) {
        boolean leveled = false;
        while (s.getExp() >= expForNextLevel(s.getLevel())) {
            s.setExp(s.getExp() - expForNextLevel(s.getLevel()));
            s.setLevel(s.getLevel() + 1);
            s.setMaxHp(s.hpForLevel());
            s.setHp(s.getMaxHp()); // hồi đầy khi lên cấp
            leveled = true;
        }
        if (leveled) {
            s.send(new Packet(PacketType.S_LEVEL_UP)
                .writeInt(s.getLevel()).writeInt(s.getMaxHp()));
            zoneManager.broadcastZone(s.getCurrentZoneId(), new Packet(PacketType.S_NOTIFY)
                .writeString(s.getCharacterName() + " đã lên cấp " + s.getLevel() + "!"));
        }
    }

    private long expForNextLevel(int level) { return level * 100L; }

    public void sendStats(PlayerSession s) {
        s.send(new Packet(PacketType.S_PLAYER_STATS)
            .writeInt(s.getLevel()).writeInt(s.getHp()).writeInt(s.getMaxHp())
            .writeLong(s.getExp()).writeLong(expForNextLevel(s.getLevel())).writeLong(s.getGold()));
    }
}
