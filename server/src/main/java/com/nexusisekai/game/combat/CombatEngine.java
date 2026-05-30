package com.nexusisekai.game.combat;

import com.nexusisekai.game.entity.MonsterInstance;
import com.nexusisekai.game.entity.Player;

import java.util.Random;

/**
 * Engine tính toán combat: damage, crit, dodge, skill effects.
 */
public class CombatEngine {

    private static final Random rng = new Random();

    // Xác suất crit cơ bản
    private static final float BASE_CRIT_RATE    = 0.05f; // 5%
    private static final float AGI_CRIT_BONUS    = 0.002f; // +0.2% per AGI
    // Xác suất dodge
    private static final float BASE_DODGE_RATE   = 0.03f;
    private static final float AGI_DODGE_BONUS   = 0.001f;

    /**
     * Player đánh thường vào monster
     */
    public static AttackResult playerAttackMonster(Player attacker, MonsterInstance target) {
        // Dodge check
        float dodge = BASE_DODGE_RATE + target.getAtk() * 0.001f;
        if (rng.nextFloat() < dodge) {
            return AttackResult.missed();
        }

        // Base damage
        int atk = attacker.getAttack();
        int def = target.getDef();
        int baseDmg = Math.max(1, atk - def / 2);

        // Crit check
        float critRate = BASE_CRIT_RATE + attacker.getAgi() * AGI_CRIT_BONUS;
        boolean isCrit = rng.nextFloat() < critRate;
        if (isCrit) baseDmg = (int)(baseDmg * 1.8f);

        // Variance ±15%
        float variance = 0.85f + rng.nextFloat() * 0.3f;
        int finalDmg = Math.max(1, (int)(baseDmg * variance));

        int actualDmg = target.takeDamage(finalDmg);
        return new AttackResult(actualDmg, isCrit, false);
    }

    /**
     * Monster đánh player
     */
    public static AttackResult monsterAttackPlayer(MonsterInstance attacker, Player target) {
        // Player dodge
        float dodge = BASE_DODGE_RATE + target.getAgi() * AGI_DODGE_BONUS;
        if (rng.nextFloat() < dodge) {
            return AttackResult.missed();
        }

        int atk = attacker.getAtk();
        int def = target.getDefense();
        int baseDmg = Math.max(1, atk - def);

        float variance = 0.85f + rng.nextFloat() * 0.3f;
        int finalDmg = Math.max(1, (int)(baseDmg * variance));

        target.takeDamage(finalDmg);
        return new AttackResult(finalDmg, false, false);
    }

    /**
     * Player dùng skill đánh monster
     */
    public static AttackResult playerSkillAttack(Player attacker, MonsterInstance target,
                                                   int skillId, int skillLevel) {
        int skillDmg = attacker.getCharClass().calcSkillDamage(skillId, skillLevel, attacker);
        int def = target.getDef();
        int finalDmg = Math.max(1, skillDmg - def / 3);

        // Skill luôn có 20% crit chance cao hơn
        boolean isCrit = rng.nextFloat() < (BASE_CRIT_RATE + attacker.getAgi() * AGI_CRIT_BONUS + 0.15f);
        if (isCrit) finalDmg = (int)(finalDmg * 2.0f);

        int actual = target.takeDamage(finalDmg);
        return new AttackResult(actual, isCrit, true);
    }

    /**
     * Tính loot khi monster chết
     * lootJson format: [{"item_id":5,"chance":0.3,"qty":1},...]
     */
    public static java.util.List<int[]> rollLoot(String lootJson) {
        java.util.List<int[]> result = new java.util.ArrayList<>();
        if (lootJson == null || lootJson.isEmpty()) return result;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode arr = mapper.readTree(lootJson);
            for (com.fasterxml.jackson.databind.JsonNode entry : arr) {
                double chance = entry.get("chance").asDouble(0.1);
                if (rng.nextDouble() < chance) {
                    int itemId = entry.get("item_id").asInt();
                    int qty    = entry.get("qty").asInt(1);
                    result.add(new int[]{itemId, qty});
                }
            }
        } catch (Exception e) { /* ignore loot parse error */ }
        return result;
    }

    // ============================================
    public static class AttackResult {
        public final int damage;
        public final boolean isCrit;
        public final boolean isSkill;
        public final boolean isDodged;

        public AttackResult(int damage, boolean isCrit, boolean isSkill) {
            this.damage   = damage;
            this.isCrit   = isCrit;
            this.isSkill  = isSkill;
            this.isDodged = false;
        }

        private AttackResult() {
            this.damage = 0; this.isCrit = false; this.isSkill = false; this.isDodged = true;
        }

        public static AttackResult missed() { return new AttackResult(); }
    }
}
