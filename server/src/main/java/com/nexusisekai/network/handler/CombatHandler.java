package com.nexusisekai.network.handler;

import com.nexusisekai.game.combat.CombatEngine;
import com.nexusisekai.game.entity.MonsterInstance;
import com.nexusisekai.game.entity.Player;
import com.nexusisekai.game.entity.PlayerSkill;
import com.nexusisekai.game.world.WorldManager;
import com.nexusisekai.network.GameSession;
import com.nexusisekai.network.PacketOpcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

public class CombatHandler {

    private static final Logger log = LoggerFactory.getLogger(CombatHandler.class);
    private static final long ATTACK_COOLDOWN_MS = 800; // 0.8s giữa 2 đòn thường

    private final GameSession session;
    private final WorldManager world;

    public CombatHandler(GameSession session, WorldManager world) {
        this.session = session;
        this.world   = world;
    }

    /**
     * C2S_ATTACK: [4 byte targetId][1 byte targetType: 0=monster,1=player]
     */
    public void handleAttack(byte[] payload) {
        Player player = session.getPlayer();
        if (!player.isAlive()) return;

        // Cooldown check
        long now = System.currentTimeMillis();
        if (now - player.getLastAttackTime() < ATTACK_COOLDOWN_MS) return;
        player.setLastAttackTime(now);

        ByteBuffer buf = ByteBuffer.wrap(payload);
        int targetId   = buf.getInt();
        int targetType = buf.get() & 0xFF;

        if (targetType == 0) {
            attackMonster(player, targetId);
        }
        // PvP sẽ mở rộng sau
    }

    private void attackMonster(Player player, int monsterId) {
        MonsterInstance monster = world.getZoneManager()
                .getMonsterInMap(player.getMapId(), monsterId);
        if (monster == null || !monster.isAlive()) return;

        // Tính damage
        CombatEngine.AttackResult result = CombatEngine.playerAttackMonster(player, monster);
        if (result.isDodged) {
            sendCombatResult(player.getCharId(), monsterId, 0, false, true);
            return;
        }

        sendCombatResult(player.getCharId(), monsterId, result.damage, result.isCrit, false);
        sendMonsterHpUpdate(monsterId, monster.getHp(), monster.getMaxHp());

        if (!monster.isAlive()) {
            handleMonsterDeath(player, monster);
        } else {
            // Monster phản đòn nếu chưa có aggro target khác
            if (monster.getAggroTarget() == -1) {
                monster.setAggroTarget(player.getCharId());
            }
        }
    }

    /**
     * C2S_USE_SKILL: [4 byte skillId][4 byte targetId][1 byte targetType]
     */
    public void handleUseSkill(byte[] payload) {
        Player player = session.getPlayer();
        if (!player.isAlive()) return;

        ByteBuffer buf = ByteBuffer.wrap(payload);
        int skillId    = buf.getInt();
        int targetId   = buf.getInt();
        int targetType = buf.get() & 0xFF;

        // Tìm skill
        PlayerSkill skill = player.getSkills().stream()
                .filter(s -> s.getSkillId() == skillId)
                .findFirst().orElse(null);
        if (skill == null) { log.warn("{} không có skill {}", player.getName(), skillId); return; }
        if (!skill.canUse()) return; // Cooldown
        if (player.getMp() < skill.getMpCost()) {
            session.sendError(PacketOpcode.S2C_COMBAT_RESULT, "Không đủ MP!");
            return;
        }

        // Trừ MP
        player.restoreMp(-skill.getMpCost());
        session.send(PacketOpcode.S2C_PLAYER_HP_UPDATE, buildHpUpdate(
                player.getCharId(), player.getHp(), player.getMaxHp(), player.getMp(), player.getMaxMp()));

        skill.use();

        if (targetType == 0) {
            MonsterInstance monster = world.getZoneManager()
                    .getMonsterInMap(player.getMapId(), targetId);
            if (monster == null || !monster.isAlive()) return;

            CombatEngine.AttackResult result = CombatEngine.playerSkillAttack(
                    player, monster, skillId, skill.getLevel());

            // Gửi hiệu ứng skill
            sendSkillEffect(skillId, monster.getX(), monster.getY());
            sendCombatResult(player.getCharId(), targetId, result.damage, result.isCrit, false);
            sendMonsterHpUpdate(targetId, monster.getHp(), monster.getMaxHp());

            if (!monster.isAlive()) {
                handleMonsterDeath(player, monster);
            }
        }
    }

    private void handleMonsterDeath(Player player, MonsterInstance monster) {
        monster.kill();

        // Roll loot
        List<int[]> loot = CombatEngine.rollLoot(monster.getLootJson());

        // Build death packet: [4 monsterInstanceId][8 killerId][4 exp][4 gold][N loot items]
        ByteBuffer resp = ByteBuffer.allocate(4 + 8 + 4 + 4 + loot.size() * 8);
        resp.putInt(monster.getInstanceId());
        resp.putLong(player.getCharId());
        resp.putInt(monster.getExpReward());
        resp.putInt(monster.getGoldReward());
        for (int[] item : loot) {
            resp.putInt(item[0]); // item_id
            resp.putInt(item[1]); // qty
        }

        // Broadcast cho map
        world.getNetworkServer().broadcastToMap(player.getMapId(),
                PacketOpcode.S2C_MONSTER_DIE, resp.array());

        // Cộng EXP cho người kill
        int levelsUp = player.gainExp(monster.getExpReward());

        // Gửi exp update
        ByteBuffer expBuf = ByteBuffer.allocate(8 + 8 + 4);
        expBuf.putLong(monster.getExpReward());
        expBuf.putLong(player.getExp());
        expBuf.putInt(player.getLevel());
        session.send(PacketOpcode.S2C_EXP_GAIN, expBuf.array());

        if (levelsUp > 0) {
            session.send(PacketOpcode.S2C_LEVEL_UP, buildHpUpdate(
                    player.getCharId(), player.getHp(), player.getMaxHp(),
                    player.getMp(), player.getMaxMp()));
        }

        // Thêm loot vào inventory player
        for (int[] item : loot) {
            world.getItemManager().giveItem(player, item[0], item[1]);
        }

        // Quest progress update
        world.getQuestManager().onMonsterKill(player, monster.getMonsterId(), monster.getInstanceId(), session);

        // Schedule respawn
        world.getZoneManager().scheduleRespawn(monster);

        log.debug("{} giết {} instance={}, +{}exp", player.getName(),
                monster.getName(), monster.getInstanceId(), monster.getExpReward());
    }

    // =========================================
    // Packet builders
    // =========================================
    private void sendCombatResult(long attackerId, long targetId, int dmg, boolean crit, boolean dodge) {
        // [8 attacker][8 target][4 dmg][1 crit][1 dodge]
        ByteBuffer buf = ByteBuffer.allocate(8 + 8 + 4 + 1 + 1);
        buf.putLong(attackerId);
        buf.putLong(targetId);
        buf.putInt(dmg);
        buf.put((byte)(crit ? 1 : 0));
        buf.put((byte)(dodge ? 1 : 0));
        world.getNetworkServer().broadcastToMap(session.getPlayer().getMapId(),
                PacketOpcode.S2C_COMBAT_RESULT, buf.array());
    }

    private void sendMonsterHpUpdate(int instanceId, int hp, int maxHp) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 4);
        buf.putInt(instanceId);
        buf.putInt(hp);
        buf.putInt(maxHp);
        world.getNetworkServer().broadcastToMap(session.getPlayer().getMapId(),
                PacketOpcode.S2C_MONSTER_HP_UPDATE, buf.array());
    }

    private void sendSkillEffect(int skillId, float x, float y) {
        ByteBuffer buf = ByteBuffer.allocate(4 + 4 + 4);
        buf.putInt(skillId);
        buf.putFloat(x);
        buf.putFloat(y);
        world.getNetworkServer().broadcastToMap(session.getPlayer().getMapId(),
                PacketOpcode.S2C_SKILL_EFFECT, buf.array());
    }

    private byte[] buildHpUpdate(long charId, int hp, int maxHp, int mp, int maxMp) {
        ByteBuffer buf = ByteBuffer.allocate(8 + 4 + 4 + 4 + 4);
        buf.putLong(charId);
        buf.putInt(hp);
        buf.putInt(maxHp);
        buf.putInt(mp);
        buf.putInt(maxMp);
        return buf.array();
    }
}
