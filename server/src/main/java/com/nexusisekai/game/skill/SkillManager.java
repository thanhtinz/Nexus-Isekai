package com.nexusisekai.game.skill;

import com.nexusisekai.database.DatabaseManager;
import com.nexusisekai.game.entity.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkillManager {
    private static final Logger log = LoggerFactory.getLogger(SkillManager.class);
    private static SkillManager INSTANCE;
    private final Map<Integer, SkillTemplate> templates = new ConcurrentHashMap<>();

    public static synchronized SkillManager getInstance() {
        if (INSTANCE == null) INSTANCE = new SkillManager();
        return INSTANCE;
    }

    public void loadAll() throws SQLException {
        templates.clear();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM skill_templates WHERE is_active=1");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                SkillTemplate t = new SkillTemplate();
                t.id = rs.getInt("id"); t.name = rs.getString("name");
                t.classId = rs.getInt("class_id"); t.skillType = rs.getString("skill_type");
                t.element = rs.getString("element"); t.baseDamage = rs.getInt("base_damage");
                t.mpCost = rs.getInt("mp_cost"); t.cooldownMs = rs.getInt("cooldown_ms");
                t.maxLevel = rs.getInt("max_level"); t.description = rs.getString("description");
                t.iconId = rs.getInt("icon_id"); t.unlockLevel = rs.getInt("unlock_level");
                templates.put(t.id, t);
            }
        }
        log.info("[SKILL] Loaded {} skill templates", templates.size());
    }

    public List<SkillTemplate> getClassSkills(int classId) {
        List<SkillTemplate> result = new ArrayList<>();
        for (SkillTemplate t : templates.values())
            if (t.classId == classId || t.classId == 0) result.add(t);
        result.sort(Comparator.comparingInt(s -> s.unlockLevel));
        return result;
    }

    // ─── Player skill operations ──────────────────────────────────

    public List<PlayerSkillData> getPlayerSkills(long charId) throws SQLException {
        List<PlayerSkillData> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT cs.skill_id, cs.skill_level, cs.skill_exp, " +
                 "st.name, st.mp_cost, st.cooldown_ms, st.base_damage, st.element, st.skill_type " +
                 "FROM character_skills cs " +
                 "JOIN skill_templates st ON st.id=cs.skill_id " +
                 "WHERE cs.char_id=? ORDER BY cs.skill_id")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PlayerSkillData d = new PlayerSkillData();
                d.skillId = rs.getInt("skill_id"); d.level = rs.getInt("skill_level");
                d.exp = rs.getInt("skill_exp");
                d.name = rs.getString("name"); d.mpCost = rs.getInt("mp_cost");
                d.cooldownMs = rs.getInt("cooldown_ms");
                d.baseDamage = rs.getInt("base_damage"); d.element = rs.getString("element");
                d.skillType = rs.getString("skill_type");
                list.add(d);
            }
        }
        return list;
    }

    public int[] getActiveSlots(long charId) throws SQLException {
        int[] slots = new int[5];
        Arrays.fill(slots, 0);
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT slot_index, skill_id FROM character_skill_slots WHERE char_id=? ORDER BY slot_index")) {
            ps.setLong(1, charId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int idx = rs.getInt("slot_index");
                if (idx >= 0 && idx < 5) slots[idx] = rs.getInt("skill_id");
            }
        }
        return slots;
    }

    public void setActiveSlot(long charId, int slot, int skillId) throws Exception {
        if (slot < 0 || slot >= 5) throw new IllegalArgumentException("Slot 0-4.");
        if (skillId != 0) {
            // Verify player has this skill
            try (Connection c = DatabaseManager.getInstance().getConnection();
                 PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM character_skills WHERE char_id=? AND skill_id=?")) {
                ps.setLong(1, charId); ps.setInt(2, skillId);
                if (!ps.executeQuery().next()) throw new IllegalStateException("Bạn chưa có skill này.");
            }
        }
        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT INTO character_skill_slots (char_id,slot_index,skill_id) VALUES (?,?,?) " +
                 "ON DUPLICATE KEY UPDATE skill_id=VALUES(skill_id)")) {
            ps.setLong(1, charId); ps.setInt(2, slot); ps.setInt(3, skillId);
            ps.executeUpdate();
        }
    }

    public void upgradeSkill(long charId, int skillId) throws Exception {
        try (Connection c = DatabaseManager.getInstance().getConnection()) {
            PreparedStatement ps = c.prepareStatement(
                "SELECT cs.skill_level, st.max_level FROM character_skills cs " +
                "JOIN skill_templates st ON st.id=cs.skill_id " +
                "WHERE cs.char_id=? AND cs.skill_id=?");
            ps.setLong(1, charId); ps.setInt(2, skillId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new IllegalStateException("Bạn chưa có skill này.");
            int currentLevel = rs.getInt("skill_level");
            int maxLevel = rs.getInt("max_level");
            if (currentLevel >= maxLevel) throw new IllegalStateException("Skill đã đạt level tối đa.");

            // Cost: 1000 * skill_level vàng
            int cost = 1000 * currentLevel;
            PreparedStatement goldPs = c.prepareStatement(
                "UPDATE characters SET gold=gold-? WHERE id=? AND gold>=?");
            goldPs.setInt(1, cost); goldPs.setLong(2, charId); goldPs.setInt(3, cost);
            int rows = goldPs.executeUpdate();
            if (rows == 0) throw new IllegalStateException("Không đủ vàng (cần " + cost + ").");

            c.prepareStatement("UPDATE character_skills SET skill_level=skill_level+1 " +
                "WHERE char_id=" + charId + " AND skill_id=" + skillId).executeUpdate();
        }
    }

    public void learnSkill(long charId, int classId, int playerLevel, int skillId) throws Exception {
        SkillTemplate t = templates.get(skillId);
        if (t == null) throw new IllegalArgumentException("Skill không tồn tại.");
        if (t.classId != classId && t.classId != 0)
            throw new IllegalStateException("Class của bạn không học được skill này.");
        if (playerLevel < t.unlockLevel)
            throw new IllegalStateException("Cần level " + t.unlockLevel + " để học skill này.");

        try (Connection c = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "INSERT IGNORE INTO character_skills (char_id,skill_id,skill_level) VALUES (?,?,1)")) {
            ps.setLong(1, charId); ps.setInt(2, skillId);
            ps.executeUpdate();
        }
    }

    public SkillTemplate getTemplate(int skillId) { return templates.get(skillId); }

    // ─── DTOs ─────────────────────────────────────────────────────

    public static class SkillTemplate {
        public int id, classId, baseDamage, mpCost, cooldownMs, maxLevel, iconId, unlockLevel;
        public String name, skillType, element, description;
    }

    public static class PlayerSkillData {
        public int skillId, level, exp, mpCost, cooldownMs, baseDamage;
        public String name, element, skillType;
    }
}
