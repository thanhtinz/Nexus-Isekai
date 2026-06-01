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
        } else if (targetType == 1) {
            attackPlayer(player, targetId);
        }
    }

    /** PvP: player đánh player — kiểm chế độ chiến đấu, áp sát thương, xử lý kill→truy nã/tù/điểm guild war. */
    private void attackPlayer(Player attacker, long victimId) {
        if (victimId == attacker.getCharId()) return;
        GameSession vSession = com.nexusisekai.network.SessionRegistry.getByCharId(victimId);
        if (vSession == null) return;
        Player victim = vSession.getPlayer();
        if (victim == null || !victim.isAlive()) return;
        if (victim.getMapId() != attacker.getMapId() || victim.getInstanceId() != attacker.getInstanceId()) return;

        try (java.sql.Connection c = com.nexusisekai.database.DatabaseManager.getInstance().getConnection()) {
            // 1. Kiểm chế độ chiến đấu (peace/guild/faction/server/berserk + nhà tù)
            String deny = CombatModeHandler.canAttack(c, attacker, victimId);
            if (deny != null) { session.sendError(PacketOpcode.S2C_COMBAT_RESULT, deny); return; }

            // 2. Áp sát thương
            CombatEngine.AttackResult result = CombatEngine.playerAttackPlayer(attacker, victim);
            sendCombatResult(attacker.getCharId(), victimId, result.damage, result.isCrit, result.isDodged);
            // cập nhật HP cho cả 2 phía
            vSession.send(PacketOpcode.S2C_PLAYER_HP_UPDATE, buildHpUpdate(
                    victim.getCharId(), victim.getHp(), victim.getMaxHp(), victim.getMp(), victim.getMaxMp()));
            session.send(PacketOpcode.S2C_PLAYER_HP_UPDATE, buildHpUpdate(
                    victim.getCharId(), victim.getHp(), victim.getMaxHp(), victim.getMp(), victim.getMaxMp()));

            // 3. Nếu mục tiêu chết → xử lý kill
            if (!victim.isAlive()) {
                handlePlayerKill(c, attacker, victim);
            }
        } catch (Exception e) {
            log.warn("Lỗi PvP {} -> {}: {}", attacker.getCharId(), victimId, e.getMessage());
        }
    }

    /** Xử lý khi 1 người chết do PvP: truy nã/tù + điểm guild war + thông báo. */
    private void handlePlayerKill(java.sql.Connection c, Player killer, Player victim) throws Exception {
        // truy nã / nhà tù (giết người vô tội → tăng wanted; wanted cao → nhốt)
        CombatModeHandler.onPlayerKill(c, killer.getCharId(), victim.getCharId(), killer.getMapId());
        // nếu nạn nhân đang bị truy nã mà chết → vào tù
        CombatModeHandler.onWantedDeath(c, victim.getCharId());

        // điểm guild war (nếu 2 người thuộc 2 guild đang trong trận trên map này)
        awardGuildWarKill(c, killer, victim);

        // broadcast chết
        ByteBuf dieMsg = io.netty.buffer.Unpooled.buffer();
        dieMsg.writeShort(PacketOpcode.S2C_PLAYER_DIE);
        dieMsg.writeLong(victim.getCharId());
        dieMsg.writeLong(killer.getCharId());
        com.nexusisekai.network.WorldBroadcast.toMap(victim.getMapId(), dieMsg);

        // hồi sinh nạn nhân về thành (giữ đơn giản: full HP, về map an toàn)
        victim.setHp(victim.getMaxHp());
        GameSession vSession = com.nexusisekai.network.SessionRegistry.getByCharId(victim.getCharId());
        if (vSession != null) {
            vSession.send(PacketOpcode.S2C_PLAYER_HP_UPDATE, buildHpUpdate(
                    victim.getCharId(), victim.getHp(), victim.getMaxHp(), victim.getMp(), victim.getMaxMp()));
        }
    }

    /** Cộng điểm guild war nếu killer & victim thuộc 2 guild đang có trận 'ongoing'. */
    private void awardGuildWarKill(java.sql.Connection c, Player killer, Player victim) throws Exception {
        long kg = killer.getGuildId(), vg = victim.getGuildId();
        if (kg <= 0 || vg <= 0 || kg == vg) return;
        java.util.Map<String,Object> war = com.nexusisekai.database.SqlSafe.queryOne(c,
                "SELECT id, guild_a, guild_b FROM guild_wars WHERE status='ongoing' " +
                "AND ((guild_a=? AND guild_b=?) OR (guild_a=? AND guild_b=?)) LIMIT 1",
                kg, vg, vg, kg);
        if (war == null) return;
        int warId = ((Number)war.get("id")).intValue();
        long guildA = ((Number)war.get("guild_a")).longValue();
        String col = (kg == guildA) ? "score_a" : "score_b";
        com.nexusisekai.database.SqlSafe.update(c, "UPDATE guild_wars SET " + col + "=" + col + "+1 WHERE id=?", warId);
        com.nexusisekai.database.SqlSafe.update(c,
                "INSERT INTO guild_war_kills (war_id,killer_char_id,victim_char_id,killer_guild,points) VALUES (?,?,?,?,1)",
                warId, killer.getCharId(), victim.getCharId(), (int)kg);
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
        // Hoạt Động X2 Tỉ Lệ Rơi: quay loot thêm lần nữa khi sự kiện đang bật
        if (ActivityHandler.activeMultiplier("x2_drop") > 1.0f) {
            loot.addAll(CombatEngine.rollLoot(monster.getLootJson()));
        }

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

        // Cộng EXP cho người kill — áp hệ số sự kiện X2 EXP (Hoạt Động) nếu đang bật
        float expMult = ActivityHandler.activeMultiplier("x2_exp");
        int expGain = (int)(monster.getExpReward() * expMult);
        int levelsUp = player.gainExp(expGain);
        // Hoạt Động: cộng tiến độ sự kiện tích EXP
        ActivityHandler.fire(player.getCharId(), "gain_exp", expGain);

        // Gửi exp update
        ByteBuffer expBuf = ByteBuffer.allocate(8 + 8 + 4);
        expBuf.putLong(expGain);
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
        ProgressionHandler.recordKill(session, monster.getMonsterId()); // bestiary tracking

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
